//
// Copyright 2019-2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.labbcat.server.db;

import java.sql.*;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.function.UnaryOperator;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.ql.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Converts AGQL expressions into SQL queries for matching participants.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class ParticipantAgqlToSql {
   
   // Attributes:
  
   /**
    * Layer schema.
    * @see #getSchema()
    * @see #setSchema(Schema)
    */
   protected Schema schema;
   /**
    * Getter for {@link #schema}.
    * @return Layer schema.
    */
   public Schema getSchema() { return schema; }
   /**
    * Setter for {@link #schema}.
    * @param schema Layer schema.
    * @return <var>this</var>.
    */
   public ParticipantAgqlToSql setSchema(Schema schema) { this.schema = schema; return this; }
  
   // Methods:
  
   /**
    * Default constructor.
    */
   public ParticipantAgqlToSql() {
   } // end of constructor
  
   /**
    * Attribute constructor.
    */
   public ParticipantAgqlToSql(Schema schema) {
      setSchema(schema);
   } // end of constructor
  
   /**
    * Transforms the given AGQL query into an SQL query.
    * @param expression The graph-matching expression, for example:
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'CC' IN labels('corpus')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'en' IN labels('transcript_language')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
    *  <li><code>list('transcript_rating').length &gt; 2</code></li>
    *  <li><code>list('participant_rating').length = 0</code></li>
    *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
    *  <li><code>my('participant_gender').label = 'NA'</code></li>
    * </ul>
    * @param sqlSelectClause The SQL expression that is to go between SELECT and FROM,
    * e.g. "speaker.name, speaker.speaker_number".
    * @param userWhereClause The expression to add to the WHERE clause to ensure the user doesn't
    * get access to data to which they're not entitled, or null.
    * @param sqlOrderClause The SQL expression that appended to the end of the SQL query,
    * e.g. "ORDER BY speaker.name LIMIT 150, 200, or null" 
    * @throws AGQLException If the expression is invalid.
    */
   public Query sqlFor(String expression, String sqlSelectClause, String userWhereClause, String sqlOrderClause)
      throws AGQLException {
      
      final Query q = new Query();
      final Stack<String> conditions = new Stack<String>();
      final Vector<String> errors = new Vector<String>();
      AGQLBaseListener listener = new AGQLBaseListener() {
            private void space() {
               if (conditions.size() > 0
                   && conditions.peek().charAt(conditions.peek().length() - 1) != ' ') {
                  conditions.push(conditions.pop() + " ");
               }
            }
            private String unquote(String s) {
               return s.substring(1, s.length() - 1);
            }
            private String attribute(String s) {
               return s.replaceAll("^(participant|transcript)_","");
            }
            private String escape(String s) {
               return s.replaceAll("\\'", "\\\\'");
            }
            @Override public void exitThisIdExpression(AGQLParser.ThisIdExpressionContext ctx) {
               space();
               conditions.push("speaker.name");
            }
            @Override public void exitThisLabelExpression(AGQLParser.ThisLabelExpressionContext ctx) {
               space();
               conditions.push("speaker.name");
            }
            @Override public void exitCorpusLabelOperand(AGQLParser.CorpusLabelOperandContext ctx) {
               space();
               conditions.push(
                  // TODO technically, a participant can be in more than one corpus
                  // TODO - this matches only the first one
                  "(SELECT corpus.corpus_name"
                  +" FROM speaker_corpus"
                  +" INNER JOIN corpus ON speaker_corpus.corpus_id = corpus.corpus_id"
                  +" WHERE speaker_corpus.speaker_number = speaker.speaker_number LIMIT 1)");
            }
            @Override public void enterCorpusLabelsExpression(AGQLParser.CorpusLabelsExpressionContext ctx) {
               space();
               conditions.push(
                  "(SELECT corpus.corpus_name"
                  +" FROM speaker_corpus"
                  +" INNER JOIN corpus ON speaker_corpus.corpus_id = corpus.corpus_id"
                  +" WHERE speaker_corpus.speaker_number = speaker.speaker_number)");
            }
            @Override public void enterLabelsExpression(AGQLParser.LabelsExpressionContext ctx) {
               space();
               String layerId = unquote(ctx.stringLiteral().quotedString.getText());
               Layer layer = getSchema().getLayer(layerId);
               if (layer == null) {
                  errors.add("Invalid layer: " + ctx.getText());
               } else {
                  String attribute = attribute(layerId);
                  if ("transcript".equals(layer.get("@class_id"))) {
                     conditions.push(
                        "(SELECT DISTINCT label"
                        +" FROM annotation_transcript"
                        +" INNER JOIN transcript_speaker"
                        +" ON annotation_transcript.ag_id = transcript_speaker.ag_id"
                        +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                        +" AND transcript_speaker.speaker_number = speaker.speaker_number"
                        +")");
                  } else if ("speaker".equals(layer.get("@class_id"))) {
                     conditions.push(
                        "(SELECT DISTINCT label"
                        +" FROM annotation_participant"
                        +" WHERE annotation_participant.layer = '"+escape(attribute)+"'"
                        +" AND annotation_participant.speaker_number = speaker.speaker_number"
                        +")");
                  } else {
                     errors.add("Can only get labels list for participant or transcript attributes: "
                                + ctx.getText());
                  }
               } // valid layer
            }
            @Override public void enterOtherLabelExpression(AGQLParser.OtherLabelExpressionContext ctx) {
               space();
               String layerId = unquote(ctx.stringLiteral().quotedString.getText());
               Layer layer = getSchema().getLayer(layerId);
               if (layer == null) {
                  errors.add("Invalid layer: " + ctx.getText());
               } else {
                  if (!"speaker".equals(layer.get("@class_id"))) {
                     errors.add("Can only get labels for participant attributes: " + ctx.getText());
                  }
                  String attribute = attribute(layerId);
                  conditions.push(
                     "(SELECT label"
                     +" FROM annotation_participant"
                     +" WHERE annotation_participant.layer = '"+escape(attribute)+"'"
                     +" AND annotation_participant.speaker_number = speaker.speaker_number"
                     +" LIMIT 1)");
               } // valid layer
            }
            @Override public void exitListLengthExpression(AGQLParser.ListLengthExpressionContext ctx) {
               space();
               String layerId = unquote(ctx.stringLiteral().quotedString.getText());
               Layer layer = getSchema().getLayer(layerId);
               if (layer == null) {
                  errors.add("Invalid layer: " + ctx.getText());
               } else {
                  String attribute = attribute(layerId);
                  if ("transcript".equals(layer.get("@class_id")))
                  {
                     conditions.push(
                        "(SELECT COUNT(*)"
                        +" FROM annotation_transcript"
                        +" INNER JOIN transcript_speaker"
                        +" ON annotation_transcript.ag_id = transcript_speaker.ag_id"
                        +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                        +" AND transcript_speaker.speaker_number = speaker.speaker_number"
                        +")");
                  } else if ("speaker".equals(layer.get("@class_id"))) {
                     conditions.push(
                        "(SELECT COUNT(*)"
                        +" FROM annotation_participant"
                        +" WHERE annotation_participant.layer = '"+escape(attribute)+"'"
                        +" AND annotation_participant.speaker_number = speaker.speaker_number"
                        +")");
                  } else {
                     errors.add("Can only get list length for participant or transcript attributes: "
                                + ctx.getText());
                  }
               } // valid layer
            }
            @Override public void enterAnnotatorsExpression(AGQLParser.AnnotatorsExpressionContext ctx) {
               space();
               String layerId = unquote(ctx.stringLiteral().quotedString.getText());
               Layer layer = getSchema().getLayer(layerId);
               if (layer == null) {
                  errors.add("Invalid layer: " + ctx.getText());
               } else {
                  String attribute = attribute(layerId);
                  if ("transcript".equals(layer.get("@class_id"))) {
                     conditions.push(
                        "(SELECT annotated_by"
                        +" FROM annotation_transcript"
                        +" INNER JOIN transcript_speaker"
                        +" ON annotation_transcript.ag_id = transcript_speaker.ag_id"
                        +" WHERE annotation_transcript.layer = '"+escape(attribute)+"'"
                        +" AND transcript_speaker.speaker_number = speaker.speaker_number"
                        +")");
                  } else if ("speaker".equals(layer.get("@class_id"))) {
                     conditions.push(
                        "(SELECT annotated_by"
                        +" FROM annotation_participant"
                        +" WHERE annotation_participant.layer = '"+escape(attribute)+"'"
                        +" AND annotation_participant.speaker_number = speaker.speaker_number"
                        +")");
                  } else {
                     errors.add("Can only get annotators for participant or transcript attributes: "
                                + ctx.getText());
                  }
               } // valid layer
            }
            @Override public void exitWhoLiteralAtom(AGQLParser.WhoLiteralAtomContext ctx) {
               conditions.push(" 'who'");
            }
            @Override public void exitGraphLiteralAtom(AGQLParser.GraphLiteralAtomContext ctx) {
               conditions.push(" 'graph'");
            }
            @Override public void exitCorpusLiteralAtom(AGQLParser.CorpusLiteralAtomContext ctx) {
               conditions.push(" 'corpus'");
            }
            @Override public void exitEpisodeLiteralAtom(AGQLParser.EpisodeLiteralAtomContext ctx) {
               conditions.push(" 'episode'");
            }
            @Override public void exitAtomList(AGQLParser.AtomListContext ctx) {
               // pop all the elements off the stack
               Stack<String> atoms = new Stack<String>();
               for (int i = 0; i < ctx.subsequentAtom().size(); i++) {
                  atoms.push(conditions.pop().trim()); // subsequentAtom
               }
               atoms.push(conditions.pop().trim()); // firstAtom

               // create a single element with all of them
               StringBuilder element = new StringBuilder();
               element.append("(");
               element.append(atoms.pop()); // firstAtom
               while (!atoms.empty()) {
                  element.append(",");
                  element.append(atoms.pop()); // subsequentAtom
               } // next atom
               element.append(")");

               // and add the whole list to conditions
               conditions.push(element.toString());
            }
            @Override public void enterComparisonOperator(AGQLParser.ComparisonOperatorContext ctx) {
               space();
               conditions.push(ctx.operator.getText().trim());
            }
            @Override public void exitPatternMatchExpression(AGQLParser.PatternMatchExpressionContext ctx) {
               if (ctx.negation != null) {
                  conditions.push(" NOT REGEXP ");
               } else {
                  conditions.push(" REGEXP ");
               }
               try
               { // ensure string literals use single, not double, quotes
                  conditions.push("'"+unquote(ctx.patternOperand.getText())+"'");
               }
               catch(Exception exception)
               { // not a string literal
                  conditions.push(ctx.patternOperand.getText());
               }
            }
            @Override public void exitIncludesExpression(AGQLParser.IncludesExpressionContext ctx) {
               // infix it - i.e. pop the last operand...
               String listOperand = conditions.pop();
               // ... insert the operator
               if (ctx.negation != null) {
                  conditions.push("NOT IN ");
               } else {
                  conditions.push("IN ");
               }
               // ... and push the operand back
               conditions.push(listOperand);
               // parse.append(ctx.singletonOperand.getText()
               //              + (ctx.negation!=null?" NOT IN ":" IN ")
               //              + ctx.listOperand.getText());
             }
            @Override public void exitLogicalOperator(AGQLParser.LogicalOperatorContext ctx) {
               space();
               conditions.push(ctx.operator.getText().trim());
            }
            @Override public void exitLiteralAtom(AGQLParser.LiteralAtomContext ctx) {
               space();
               try { // ensure string literals use single, not double, quotes
                  conditions.push("'"+unquote(ctx.literal().stringLiteral().getText())+"'");
               } catch(Exception exception) { // not a string literal
                  conditions.push(ctx.getText());
               }
            }
            @Override public void exitIdentifierAtom(AGQLParser.IdentifierAtomContext ctx) {
               space();
               conditions.push(ctx.getText());
            }
            @Override public void visitErrorNode(ErrorNode node) {
               errors.add(node.getText());
            }
         };
      AGQLLexer lexer = new AGQLLexer(CharStreams.fromString(expression));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      AGQLParser parser = new AGQLParser(tokens);
      AGQLParser.BooleanExpressionContext tree = parser.booleanExpression();
      if (expression != null && expression.trim().length() > 0)
      {
         ParseTreeWalker.DEFAULT.walk(listener, tree);
      }

      if (errors.size() > 0) {
         throw new AGQLException(expression, errors);
      }
      StringBuilder sql = new StringBuilder();
      sql.append("SELECT ");
      sql.append(sqlSelectClause);
      sql.append(" FROM speaker");
      if (conditions.size() > 0) {
         sql.append(" WHERE ");
         for (String condition : conditions) sql.append(condition);
      }
      if (userWhereClause != null && userWhereClause.trim().length() > 0) {
         sql.append(conditions.size() > 0?" AND ":" WHERE ");
         sql.append(userWhereClause);
      }

      // Don't process the order clause for now - it's SQL
      // // now order clause
      // StringBuilder order = new StringBuilder();
      // for (String part : orderClause.split(","))
      // {
      //   order.append(order.length() == 0?" ORDER BY":",");
      //   conditions.setLength(0);
      //   lexer.setInputStream(CharStreams.fromString(part));
      //   tokens = new CommonTokenStream(lexer);
      //   parser = new AGQLParser(tokens);
      //   tree = parser.query();
      //   ParseTreeWalker.DEFAULT.walk(listener, tree);
      //   order.append(" ");
      //   order.append(conditions);
      // } // next orderClause part
      // sql.append(order);
      if (sqlOrderClause.length() > 0) {
         sql.append(" ");
         sql.append(sqlOrderClause);
      }
    
      q.sql = sql.toString();
      return q;
   } // end of sqlFor()

   /** 
    * Encapsulates the results of {@link #sqlFor(String,String,String,String)} including the SQL.
    * string and the parameters to set.
    */
   public static class Query {
      
      public String sql;
      public List<Object> parameters = new Vector<Object>();
    
      /**
       * Creates a prepared statement from the sql string and the parameters.
       * @param db A connection to the database.
       * @return A prepared statement with parameters set.
       * @throws SqlException
       */
      public PreparedStatement prepareStatement(Connection db)
         throws SQLException {
         
         PreparedStatement query = db.prepareStatement(sql);
         int p = 1;
         for (Object parameter : parameters) {
            if (parameter instanceof Integer) query.setInt(p++, (Integer)parameter);
            else if (parameter instanceof Double) query.setDouble(p++, (Double)parameter);
            else query.setString(p++, parameter.toString());
         } // next parameter
         return query;
      } // end of prepareStatement()
   }
} // end of class ParticipantAgqlToSql
