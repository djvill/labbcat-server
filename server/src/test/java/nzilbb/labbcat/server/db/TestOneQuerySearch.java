//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//

package nzilbb.labbcat.server.db;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.Vector;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.labbcat.server.search.Column;
import nzilbb.labbcat.server.search.LayerMatch;
import nzilbb.labbcat.server.search.Matrix;

public class TestOneQuerySearch {

  /** Ensure the simplest search structure generates the correct SQL. */
  @Test public void oneWordSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(
          new LayerMatch().setId("word").setPattern("needle").setTarget(true))));
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_0.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_0.start_anchor_id,"
      +" search_0_0.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_0.annotation_id AS target_annotation_id,"
      +" search_0_0.turn_annotation_id AS turn_annotation_id,"
      +" search_0_0.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_0.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_0.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_0 search_0_0"
      +"  ON search_0_0.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_0.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_0.turn_annotation_id, search_0_0.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());
  }
  
  /** Ensure a simple word search on a word layer that's not "word" generates the correct SQL. */
  @Test public void oneWordLayerSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(
          new LayerMatch().setId("orthography")
          .setNot(true) // add negation to prevent optimisation
          .setPattern("needle"))));
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label NOT REGEXP  ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_NOT_^(needle)$", search.getDescription());
  }
  
  /** Ensure an orthography-only searches produce optimised SQL. */
  @Test public void optimisedOrthographySearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(
          new LayerMatch().setId("orthography").setPattern("needle"))));
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateOrthographySql(parameters, getSchema());
    assertEquals(
      "one column",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id,"
      +" turn_annotation_id,"
      +" first_matched_word_annotation_id,"
      +" last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, token_0.ag_id AS ag_id,"
      +" 0 AS speaker_number, token_0.start_anchor_id, token_0.end_anchor_id,"
      +" 0, NULL AS segment_annotation_id, token_0.word_annotation_id AS target_annotation_id,"
      +" token_0.turn_annotation_id AS turn_annotation_id,"
      +" token_0.word_annotation_id AS first_matched_word_annotation_id,"
      +" token_0.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_2_', token_0.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_2 token_0"
      +" WHERE token_0.label REGEXP ?"
      +" ORDER BY token_0.turn_annotation_id, token_0.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("Description", "_^(needle)$", search.getDescription());

    // multi-column
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(new LayerMatch().setId("orthography").setPattern("testing")))
      .addColumn(
        new Column().addLayerMatch(new LayerMatch().setId("orthography").setPattern("1")))
      .addColumn(
        new Column().addLayerMatch(new LayerMatch().setId("orthography").setPattern("2")))
      .addColumn(
        new Column().addLayerMatch(new LayerMatch().setId("orthography").setPattern("3"))));
    parameters = new Vector<Object>();
    sql = search.generateOrthographySql(parameters, getSchema());
    assertEquals(
      "multi column",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id,"
      +" turn_annotation_id,"
      +" first_matched_word_annotation_id,"
      +" last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, token_0.ag_id AS ag_id,"
      +" 0 AS speaker_number, token_0.start_anchor_id, token_0.end_anchor_id,"
      +" 0, NULL AS segment_annotation_id, token_0.word_annotation_id AS target_annotation_id,"
      +" token_0.turn_annotation_id AS turn_annotation_id,"
      +" token_0.word_annotation_id AS first_matched_word_annotation_id,"
      +" token_3.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_2_', token_0.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_2 token_0"
      +" INNER JOIN annotation_layer_2 token_1"
      +" ON token_1.turn_annotation_id = token_0.turn_annotation_id"
      +" AND token_1.ordinal_in_turn = token_0.ordinal_in_turn + 1"
      +" INNER JOIN annotation_layer_2 token_2"
      +" ON token_2.turn_annotation_id = token_0.turn_annotation_id"
      +" AND token_2.ordinal_in_turn = token_1.ordinal_in_turn + 1"
      +" INNER JOIN annotation_layer_2 token_3"
      +" ON token_3.turn_annotation_id = token_0.turn_annotation_id"
      +" AND token_3.ordinal_in_turn = token_2.ordinal_in_turn + 1"
      +" WHERE token_0.label REGEXP ?"
      +" AND token_1.label REGEXP ?"
      +" AND token_2.label REGEXP ?"
      +" AND token_3.label REGEXP ?"
      +" ORDER BY token_0.turn_annotation_id, token_0.ordinal_in_turn",
      sql);
    assertEquals("multi column - number of parameters" + parameters, 4, parameters.size());
    assertEquals("^(testing)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(1)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals("^(2)$", parameters.get(2));
    assertTrue(parameters.get(2) instanceof String);
    assertEquals("^(3)$", parameters.get(3));
    assertTrue(parameters.get(3) instanceof String);
    
    assertEquals("Description", "_^(testing)$_^(1)$_^(2)$_^(3)$", search.getDescription());
  }
  
  /** Ensure a one-target-span-only searches produces optimised SQL. */
  @Test public void optimisedSpanSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    Schema schema = getSchema();
    Layer spanLayer = schema.getLayer("topic");
    LayerMatch match = new LayerMatch()
      .setId(spanLayer.getId()).setPattern("needle").setTarget(true);
    match.setNullBooleans();
    match.ensurePatternAnchored();
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(match)));
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateOneSpanSql(parameters, schema, spanLayer, match);
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id,"
      +" turn_annotation_id, first_matched_word_annotation_id,"
      +" last_matched_word_annotation_id, complete, target_annotation_uid)"
      +" SELECT ?, token.ag_id AS ag_id, 0 AS speaker_number,"
      +" token.start_anchor_id, token.end_anchor_id,"
      +" NULL AS defining_annotation_id,"
      +" NULL AS segment_annotation_id,"
      +" token.annotation_id AS target_annotation_id,"
      +" NULL AS turn_annotation_id,"
      +" NULL AS first_matched_word_annotation_id,"
      +" NULL AS last_matched_word_annotation_id,"
      +" 0 AS complete,"
      +" CONCAT('e_',30,'_', token.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_30 token"
      +" INNER JOIN anchor start ON token.start_anchor_id = start.anchor_id"
      +" WHERE token.label REGEXP ? ORDER BY token.ag_id, start.offset",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());
  }
  
  /** Ensure searching main participant utterances only generates the correct SQL. */
  @Test public void mainParticipant() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(
          new LayerMatch().setId("orthography").setPattern("needle"))));

    // main participant
    Vector<Object> parameters = new Vector<Object>();
    search.setMainParticipantOnly(true);
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN transcript_speaker"
      +" ON transcript_speaker.ag_id = turn.ag_id"
      +" AND turn.label REGEXP '^[0-9]+$'"
      +" AND transcript_speaker.speaker_number = CAST(turn.label AS SIGNED)"
      +"  /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  AND transcript_speaker.main_speaker = 1"
      +" /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());
  }
  
  /** Ensure searching aligned words only generates the correct SQL. */
  @Test public void alignedWords() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix().addColumn(
        new Column().addLayerMatch(
          new LayerMatch().setId("orthography").setPattern("needle"))));

    // main participant
    Vector<Object> parameters = new Vector<Object>();
    search.setAnchorConfidenceThreshold((byte)50);
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start"
      +" ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" AND word_0_start.alignment_status >= 50"
      +" INNER JOIN anchor word_0_end"
      +" ON word_0_end.anchor_id = search_0_2.end_anchor_id"
      +" AND word_0_end.alignment_status >= 50"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());
  }
  
  /** Ensure a one-column search with multiple word layers generates the correct SQL. */
  @Test public void multiWordLayerSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix().addColumn(
        new Column()
        .addLayerMatch(new LayerMatch()
                       .setId("phonemes").setPattern("[^cCEFHiIPqQuUV0123456789~#\\{\\$@].*"))
        .addLayerMatch(new LayerMatch()
                       .setId("orthography").setPattern("[aeiou].*"))
        ));
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_52.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_52.start_anchor_id,"
      +" search_0_52.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_52.word_annotation_id AS target_annotation_id,"
      +" search_0_52.turn_annotation_id AS turn_annotation_id,"
      +" search_0_52.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_52.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_52.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_52 search_0_52"
      +"  ON search_0_52.turn_annotation_id = turn.annotation_id"
      +"  AND CAST(search_0_52.label AS BINARY)  REGEXP BINARY ?"
      +" INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.word_annotation_id = search_0_52.word_annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_52.turn_annotation_id, search_0_52.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^([^cCEFHiIPqQuUV0123456789~#\\{\\$@].*)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^([aeiou].*)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description",
                 "_^([^cCEFHiIPqQuUV0123456789~#\\{\\$@].*)$_^([aeiou].*)$",
                 search.getDescription());
  }
  
  /** Ensure a multi-column search generates the correct SQL. */
  @Test public void multiColumnSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("knitting"))
                 .setAdj(2))
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle")))
      );
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_1_2.label  REGEXP  ?"
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 3, parameters.size());
    assertEquals("^(knitting)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(needle)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals("adjacency", Integer.valueOf(2), parameters.get(2));
    assertTrue(parameters.get(2) instanceof Integer);
    
    assertEquals("Description", "_^(knitting)$_^(needle)$", search.getDescription());
  }
  
  /** Ensure searches with min and max generate the correct SQL. */
  @Test public void numericSearches() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();

    // min only
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("syllableCount")
                                .setMin("2")))
      );
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "min only",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_186.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_186.start_anchor_id,"
      +" search_0_186.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_186.word_annotation_id AS target_annotation_id,"
      +" search_0_186.turn_annotation_id AS turn_annotation_id,"
      +" search_0_186.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_186.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_186.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_186 search_0_186"
      +"  ON search_0_186.turn_annotation_id = turn.annotation_id"
      +"  AND CAST(search_0_186.label AS DECIMAL) >= ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_186.turn_annotation_id, search_0_186.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals(Double.valueOf(2), parameters.get(0));
    assertTrue(parameters.get(0) instanceof Double);
    
    assertEquals("Description", "_2", search.getDescription());

    // max only
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("syllableCount")
                                .setMax("3")))
      );
    parameters.clear();
    sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "max only",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_186.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_186.start_anchor_id,"
      +" search_0_186.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_186.word_annotation_id AS target_annotation_id,"
      +" search_0_186.turn_annotation_id AS turn_annotation_id,"
      +" search_0_186.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_186.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_186.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_186 search_0_186"
      +"  ON search_0_186.turn_annotation_id = turn.annotation_id"
      +"  AND CAST(search_0_186.label AS DECIMAL) < ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_186.turn_annotation_id, search_0_186.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals(Double.valueOf(3), parameters.get(0));
    assertTrue(parameters.get(0) instanceof Double);

    assertEquals("Description", "_3", search.getDescription());

    // min and max
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("syllableCount")
                                .setMin("2")
                                .setMax("3")))
      );
    parameters.clear();
    sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "min and max",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_186.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_186.start_anchor_id,"
      +" search_0_186.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_186.word_annotation_id AS target_annotation_id,"
      +" search_0_186.turn_annotation_id AS turn_annotation_id,"
      +" search_0_186.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_186.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_186.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_186 search_0_186"
      +"  ON search_0_186.turn_annotation_id = turn.annotation_id"
      +"  AND CAST(search_0_186.label AS DECIMAL) >= ?"
      +"  AND CAST(search_0_186.label AS DECIMAL) < ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_186.turn_annotation_id, search_0_186.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals(Double.valueOf(2), parameters.get(0));
    assertTrue(parameters.get(0) instanceof Double);
    assertEquals(Double.valueOf(3), parameters.get(1));
    assertTrue(parameters.get(1) instanceof Double);

    assertEquals("Description", "_2_3", search.getDescription());
  }
  
  /** Ensure searches with word-start anchoring generate the correct SQL. */
  @Test public void anchorStart() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();

    // phrase layer
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("language").setPattern("es")
                                .setAnchorStart(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("mate")))
      );
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // no word linked layers
      p -> "", t -> "");
    assertEquals(
      "not word-linked phrase layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_20 search_0_20"
      +"  INNER JOIN anchor meta_0_start_20"
      +"  ON meta_0_start_20.anchor_id = search_0_20.start_anchor_id"
      +"  INNER JOIN anchor meta_0_end_20"
      +"  ON meta_0_end_20.anchor_id = search_0_20.end_anchor_id)"
      +"  ON search_0_20.ag_id = search_0_2.ag_id"
      +"  AND search_0_20.turn_annotation_id = search_0_2.turn_annotation_id"
      +"  AND meta_0_start_20.offset <= word_0_start.offset"
      +"  AND meta_0_end_20.offset > word_0_start.offset"
      +"  AND search_0_20.label  REGEXP  ?"
      +"  AND NOT EXISTS (SELECT other_word.label"
      +"  FROM annotation_layer_0  other_word"
      +"  INNER JOIN anchor other_word_start"
      +"  ON other_word_start.anchor_id = other_word.start_anchor_id"
      +"  WHERE other_word.turn_annotation_id = search_0_2.turn_annotation_id"
      +"  AND other_word_start.offset >= meta_0_start_20.offset"
      +"  AND other_word_start.offset < meta_0_end_20.offset"
      +"  AND other_word_start.offset < word_0_start.offset )"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(mate)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(es)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);

    assertEquals("Description", "_^(mate)$_^(es)$", search.getDescription());

    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> l.getId().equals("language"), // language layer is word-linked
      p -> "", t -> "");
    assertEquals(
      "word-linked phrase layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN annotation_layer_20 search_0_20"
      +" ON search_0_20.start_anchor_id = search_0_2.start_anchor_id"
      +" AND search_0_20.label  REGEXP  ? "
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(mate)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(es)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(mate)$_^(es)$", search.getDescription());

    // span layer
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("topic").setPattern("haystack")
                                .setAnchorStart(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle")))
      );
    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // no word linked layers
      p -> "", t -> "");
    assertEquals(
      "not word-linked span layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_30 search_0_30"
      +"  INNER JOIN anchor meta_0_start_30"
      +"  ON meta_0_start_30.anchor_id = search_0_30.start_anchor_id"
      +"  INNER JOIN anchor meta_0_end_30"
      +"  ON meta_0_end_30.anchor_id = search_0_30.end_anchor_id)"
      +"  ON search_0_30.ag_id = search_0_2.ag_id"
      +"  AND meta_0_start_30.offset <= word_0_start.offset"
      +"  AND meta_0_end_30.offset > word_0_start.offset"
      +"  AND search_0_30.label  REGEXP  ?"
      +"  AND ( search_0_2.start_anchor_id = search_0_30.start_anchor_id OR NOT EXISTS"
      +" (SELECT other_word.label"
      +"  FROM annotation_layer_0  other_word"
      +"  INNER JOIN annotation_layer_11  other_turn"
      +"  ON other_word.turn_annotation_id = other_turn.annotation_id"
      +"  INNER JOIN anchor other_word_start"
      +"  ON other_word_start.anchor_id = other_word.start_anchor_id"
      +"  WHERE other_word.ag_id = search_0_2.ag_id  AND other_turn.label = turn.label"
      +"  AND other_word_start.offset >= meta_0_start_30.offset"
      +"  AND other_word_start.offset < meta_0_end_30.offset"
      +"  AND other_word_start.offset < word_0_start.offset ) )"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(haystack)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(needle)$_^(haystack)$", search.getDescription());

    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> l.getId().equals("topic"), // topic layer is word-linked
      p -> "", t -> "");
    assertEquals(
      "word-linked span layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN annotation_layer_30 search_0_30"
      +" ON search_0_30.start_anchor_id = search_0_2.start_anchor_id"
      +" AND search_0_30.label  REGEXP  ? "
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(haystack)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(needle)$_^(haystack)$", search.getDescription());

    // turn start
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle"))
                 .addLayerMatch(new LayerMatch().setId("turn").setAnchorStart(true)))
      );
    parameters = new Vector<Object>();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // this not called for turn
      p -> "", t -> "");
    assertEquals(
      "turn start",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  AND search_0_2.ordinal_in_turn = 1"
      +" /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());

    // utterance start
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle"))
                 .addLayerMatch(new LayerMatch().setId("utterance").setAnchorStart(true)))
      );
    parameters = new Vector<Object>();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // this not called for utterance
      p -> "", t -> "");
    assertEquals(
      "utterance start",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_12 line_0"
      +"  INNER JOIN anchor line_0_start  ON line_0_start.anchor_id = line_0.start_anchor_id"
      +"  INNER JOIN anchor line_0_end  ON line_0_end.anchor_id = line_0.end_anchor_id)"
      +" ON line_0.turn_annotation_id = search_0_2.turn_annotation_id"
      +"  AND line_0_start.offset <= word_0_start.offset"
      +"  AND line_0_end.offset > word_0_start.offset"
      +" LEFT OUTER JOIN annotation_layer_0 last_word"
      +" ON last_word.turn_annotation_id = search_0_2.turn_annotation_id"
      +" AND last_word.ordinal_in_turn = search_0_2.ordinal_in_turn - 1"
      +"  LEFT OUTER JOIN anchor last_word_start"
      +"  ON last_word_start.anchor_id = last_word.start_anchor_id"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  AND (last_word.annotation_id IS NULL OR last_word_start.offset < line_0_start.offset)"
      +" /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());

  }
  
  /** Ensure one-column searches with word-end anchoring generate the correct SQL. */
  @Test public void oneColumAnchorEnd() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();

    // phrase layer
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("language").setPattern("es")
                                .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("mate")))
      );
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // no word linked layers
      p -> "", t -> "");
    assertEquals(
      "not word-linked phrase layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_20 search_0_20"
      +"  INNER JOIN anchor meta_0_start_20"
      +"  ON meta_0_start_20.anchor_id = search_0_20.start_anchor_id"
      +"  INNER JOIN anchor meta_0_end_20"
      +"  ON meta_0_end_20.anchor_id = search_0_20.end_anchor_id)"
      +"  ON search_0_20.ag_id = search_0_2.ag_id"
      +"  AND search_0_20.turn_annotation_id = search_0_2.turn_annotation_id"
      +"  AND meta_0_start_20.offset <= word_0_start.offset"
      +"  AND meta_0_end_20.offset > word_0_start.offset"
      +"  AND search_0_20.label  REGEXP  ?  AND NOT EXISTS"
      +" (SELECT other_word.label"
      +"  FROM annotation_layer_0  other_word"
      +"  INNER JOIN anchor other_word_start"
      +"  ON other_word_start.anchor_id = other_word.start_anchor_id"
      +"  WHERE other_word.turn_annotation_id = search_0_2.turn_annotation_id"
      +"  AND other_word_start.offset >= meta_0_start_20.offset"
      +"  AND other_word_start.offset < meta_0_end_20.offset"
      +"  AND other_word_start.offset > word_0_start.offset )"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(mate)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(es)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(mate)$_^(es)$", search.getDescription());

    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> l.getId().equals("language"), // language layer is word-linked
      p -> "", t -> "");
    assertEquals(
      "word-linked phrase layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN annotation_layer_20 search_0_20"
      +" ON search_0_20.end_anchor_id = search_0_2.end_anchor_id"
      +" AND search_0_20.label  REGEXP  ? "
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(mate)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(es)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(mate)$_^(es)$", search.getDescription());

    // span layer
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("topic").setPattern("haystack")
                                .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle")))
      );
    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // no word linked layers
      p -> "", t -> "");
    assertEquals(
      "not word-linked span layer", // TODO this is not valid: Unknown column 'search_30.end_anchor_id' in 'on clause' - looks like it should be search_0_30.end_anchor_id
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_30 search_0_30"
      +"  INNER JOIN anchor meta_0_start_30"
      +"  ON meta_0_start_30.anchor_id = search_0_30.start_anchor_id"
      +"  INNER JOIN anchor meta_0_end_30"
      +"  ON meta_0_end_30.anchor_id = search_0_30.end_anchor_id)"
      +"  ON search_0_30.ag_id = search_0_2.ag_id"
      +"  AND meta_0_start_30.offset <= word_0_start.offset"
      +"  AND meta_0_end_30.offset > word_0_start.offset"
      +"  AND search_0_30.label  REGEXP  ?  AND"
      +" ( search_0_2.end_anchor_id = search_30.end_anchor_id OR NOT EXISTS"
      +" (SELECT other_word.label"
      +"  FROM annotation_layer_0  other_word"
      +"  INNER JOIN anchor other_word_start"
      +"  ON other_word_start.anchor_id = other_word.start_anchor_id"
      +"  WHERE other_word.ag_id = search_0_2.ag_id"
      +"  AND other_word_start.offset >= meta_0_start_30.offset"
      +"  AND other_word_start.offset < meta_0_end_30.offset"
      +"  AND other_word_start.offset > word_0_start.offset ) )"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(haystack)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(needle)$_^(haystack)$", search.getDescription());

    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> l.getId().equals("topic"), // topic layer is word-linked
      p -> "", t -> "");
    assertEquals(
      "word-linked span layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN annotation_layer_30 search_0_30"
      +" ON search_0_30.end_anchor_id = search_0_2.end_anchor_id"
      +" AND search_0_30.label  REGEXP  ? "
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(haystack)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(needle)$_^(haystack)$", search.getDescription());

    // turn end
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle"))
                 .addLayerMatch(new LayerMatch().setId("turn").setAnchorEnd(true)))
      );
    parameters = new Vector<Object>();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // this not called for turn
      p -> "", t -> "");
    assertEquals(
      "turn end",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" LEFT OUTER JOIN annotation_layer_0  next_word_0"
      +" ON next_word_0.turn_annotation_id = search_0_2.turn_annotation_id"
      +" AND next_word_0.ordinal_in_turn = search_0_2.ordinal_in_turn + 1"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  AND next_word_0.annotation_id IS NULL"
      +" /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());

    // utterance end
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle"))
                 .addLayerMatch(new LayerMatch().setId("utterance").setAnchorEnd(true)))
      );
    parameters = new Vector<Object>();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // this not called for utterance
      p -> "", t -> "");
    assertEquals(
      "utterance end",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_0_start ON word_0_start.anchor_id = search_0_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_12 line_0"
      +"  INNER JOIN anchor line_0_start  ON line_0_start.anchor_id = line_0.start_anchor_id"
      +"  INNER JOIN anchor line_0_end  ON line_0_end.anchor_id = line_0.end_anchor_id)"
      +" ON line_0.turn_annotation_id = search_0_2.turn_annotation_id"
      +"  AND line_0_start.offset <= word_0_start.offset"
      +"  AND line_0_end.offset > word_0_start.offset"
      +" LEFT OUTER JOIN annotation_layer_0  next_word_0"
      +" ON next_word_0.turn_annotation_id = search_0_2.turn_annotation_id"
      +" AND next_word_0.ordinal_in_turn = search_0_2.ordinal_in_turn + 1"
      +"  LEFT OUTER JOIN anchor next_word_0_start"
      +"  ON next_word_0_start.anchor_id = next_word_0.start_anchor_id"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  AND (next_word_0.annotation_id IS NULL"
      +" OR next_word_0_start.offset >= line_0_end.offset)"
      +" /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(needle)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(needle)$", search.getDescription());

  }
  
  /** Ensure multi-column searches with word-end anchoring generate the correct SQL. */
  @Test public void multiColumnAnchorEnd() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();

    // phrase layer
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("yerba")))
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("language").setPattern("es")
                                .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("mate")))
      );
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // no word linked layers
      p -> "", t -> "");
    assertEquals(
      "not word-linked phrase layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id,"
      +" turn_annotation_id, first_matched_word_annotation_id, last_matched_word_annotation_id,"
      +" complete, target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_1_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_1_start ON word_1_start.anchor_id = search_1_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_20 search_1_20"
      +"  INNER JOIN anchor meta_1_start_20"
      +"  ON meta_1_start_20.anchor_id = search_1_20.start_anchor_id"
      +"  INNER JOIN anchor meta_1_end_20"
      +"  ON meta_1_end_20.anchor_id = search_1_20.end_anchor_id)"
      +"  ON search_1_20.ag_id = search_1_2.ag_id"
      +"  AND search_1_20.turn_annotation_id = search_1_2.turn_annotation_id"
      +"  AND meta_1_start_20.offset <= word_1_start.offset"
      +"  AND meta_1_end_20.offset > word_1_start.offset"
      +"  AND search_1_20.label  REGEXP  ?  AND NOT EXISTS"
      +" (SELECT other_word.label"
      +"  FROM annotation_layer_0  other_word"
      +"  INNER JOIN anchor other_word_start"
      +"  ON other_word_start.anchor_id = other_word.start_anchor_id"
      +"  WHERE other_word.turn_annotation_id = search_1_2.turn_annotation_id"
      +"  AND other_word_start.offset >= meta_1_start_20.offset"
      +"  AND other_word_start.offset < meta_1_end_20.offset"
      +"  AND other_word_start.offset > word_1_start.offset )"
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 4, parameters.size());
    assertEquals("^(yerba)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(mate)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals("^(es)$", parameters.get(2));
    assertTrue(parameters.get(2) instanceof String);
    assertEquals("adjacency", Integer.valueOf(1), parameters.get(3));
    assertTrue(parameters.get(3) instanceof Integer);
    
    assertEquals("Description", "_^(yerba)$_^(mate)$_^(es)$", search.getDescription());

    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> l.getId().equals("language"), // language layer is word-linked
      p -> "", t -> "");
    assertEquals(
      "word-linked phrase layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id,"
      +" turn_annotation_id, first_matched_word_annotation_id, last_matched_word_annotation_id,"
      +" complete, target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_1_2.label  REGEXP  ?"
      +" INNER JOIN annotation_layer_20 search_1_20"
      +" ON search_1_20.end_anchor_id = search_1_2.end_anchor_id"
      +" AND search_1_20.label  REGEXP  ? "
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 4, parameters.size());
    assertEquals("^(yerba)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(mate)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals("^(es)$", parameters.get(2));
    assertTrue(parameters.get(2) instanceof String);
    assertEquals("adjacency", Integer.valueOf(1), parameters.get(3));
    assertTrue(parameters.get(3) instanceof Integer);
    
    assertEquals("Description", "_^(yerba)$_^(mate)$_^(es)$", search.getDescription());

    // span layer
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("knitting")))
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("topic").setPattern("haystack")
                                .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle")))
      );
    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // no word linked layers
      p -> "", t -> "");
    assertEquals(
      "not word-linked span layer", // TODO this is not valid: Unknown column 'search_30.end_anchor_id' in 'on clause' - looks like it should be search_0_30.end_anchor_id
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_1_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_1_start ON word_1_start.anchor_id = search_1_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_30 search_1_30"
      +"  INNER JOIN anchor meta_1_start_30"
      +"  ON meta_1_start_30.anchor_id = search_1_30.start_anchor_id"
      +"  INNER JOIN anchor meta_1_end_30"
      +"  ON meta_1_end_30.anchor_id = search_1_30.end_anchor_id)"
      +"  ON search_1_30.ag_id = search_1_2.ag_id"
      +"  AND meta_1_start_30.offset <= word_1_start.offset"
      +"  AND meta_1_end_30.offset > word_1_start.offset"
      +"  AND search_1_30.label  REGEXP  ?"
      +"  AND ( search_1_2.end_anchor_id = search_30.end_anchor_id OR NOT EXISTS"
      +" (SELECT other_word.label"
      +"  FROM annotation_layer_0  other_word"
      +"  INNER JOIN anchor other_word_start"
      +"  ON other_word_start.anchor_id = other_word.start_anchor_id"
      +"  WHERE other_word.ag_id = search_1_2.ag_id"
      +"  AND other_word_start.offset >= meta_1_start_30.offset"
      +"  AND other_word_start.offset < meta_1_end_30.offset"
      +"  AND other_word_start.offset > word_1_start.offset ) )"
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 4, parameters.size());
    assertEquals("^(knitting)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(needle)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals("^(haystack)$", parameters.get(2));
    assertTrue(parameters.get(2) instanceof String);
    assertEquals(Integer.valueOf(1), parameters.get(3));
    assertTrue(parameters.get(3) instanceof Integer);
    
    assertEquals("Description", "_^(knitting)$_^(needle)$_^(haystack)$", search.getDescription());

    parameters.clear();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> l.getId().equals("topic"), // topic layer is word-linked
      p -> "", t -> "");
    assertEquals(
      "word-linked span layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_1_2.label  REGEXP  ?"
      +" INNER JOIN annotation_layer_30 search_1_30"
      +" ON search_1_30.end_anchor_id = search_1_2.end_anchor_id"
      +" AND search_1_30.label  REGEXP  ? "
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 4, parameters.size());
    assertEquals("^(knitting)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(needle)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals("^(haystack)$", parameters.get(2));
    assertTrue(parameters.get(2) instanceof String);
    assertEquals(Integer.valueOf(1), parameters.get(3));
    assertTrue(parameters.get(3) instanceof Integer);
    
    assertEquals("Description", "_^(knitting)$_^(needle)$_^(haystack)$", search.getDescription());

    // turn end
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("knitting")))
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle"))
                 .addLayerMatch(new LayerMatch().setId("turn").setAnchorEnd(true)))
      );
    parameters = new Vector<Object>();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // this not called for turn
      p -> "", t -> "");
    assertEquals(
      "turn end",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_1_2.label  REGEXP  ?"
      +" LEFT OUTER JOIN annotation_layer_0  next_word_1"
      +" ON next_word_1.turn_annotation_id = search_1_2.turn_annotation_id"
      +" AND next_word_1.ordinal_in_turn = search_1_2.ordinal_in_turn + 1"
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +" AND next_word_1.annotation_id IS NULL"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 3, parameters.size());
    assertEquals("^(knitting)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(needle)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals(Integer.valueOf(1), parameters.get(2));
    assertTrue(parameters.get(2) instanceof Integer);
    
    assertEquals("Description", "_^(knitting)$_^(needle)$", search.getDescription());

    // utterance end
    search.setMatrix(
      new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("knitting")))
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("needle"))
                 .addLayerMatch(new LayerMatch().setId("utterance").setAnchorEnd(true)))
      );
    parameters = new Vector<Object>();
    sql = search.generateSql(
      parameters, getSchema(),
      l -> false, // this not called for utterance
      p -> "", t -> "");
    assertEquals(
      "utterance end",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, NULL AS segment_annotation_id,"
      +" search_0_2.word_annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_1_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('ew_0_', search_0_2.word_annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_2 search_0_2"
      +"  ON search_0_2.turn_annotation_id = turn.annotation_id"
      +"  AND search_0_2.label  REGEXP  ?"
      +" /* subsequent columns */"
      +"  INNER JOIN annotation_layer_2 search_1_2"
      +"  ON search_1_2.turn_annotation_id = turn.annotation_id  AND search_1_2.label  REGEXP  ?"
      +" INNER JOIN anchor word_1_start ON word_1_start.anchor_id = search_1_2.start_anchor_id"
      +" INNER JOIN (annotation_layer_12 line_1"
      +"  INNER JOIN anchor line_1_start  ON line_1_start.anchor_id = line_1.start_anchor_id"
      +"  INNER JOIN anchor line_1_end  ON line_1_end.anchor_id = line_1.end_anchor_id)"
      +" ON line_1.turn_annotation_id = search_1_2.turn_annotation_id"
      +"  AND line_1_start.offset <= word_1_start.offset"
      +"  AND line_1_end.offset > word_1_start.offset"
      +" LEFT OUTER JOIN annotation_layer_0  next_word_1"
      +" ON next_word_1.turn_annotation_id = search_1_2.turn_annotation_id"
      +" AND next_word_1.ordinal_in_turn = search_1_2.ordinal_in_turn + 1"
      +"  LEFT OUTER JOIN anchor next_word_1_start"
      +"  ON next_word_1_start.anchor_id = next_word_1.start_anchor_id"
      +" WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  /* column _1: */"
      +"  AND search_1_2.ordinal_in_turn"
      +" BETWEEN search_0_2.ordinal_in_turn + 1 AND search_0_2.ordinal_in_turn + ?"
      +" AND (next_word_1.annotation_id IS NULL OR next_word_1_start.offset >= line_1_end.offset)"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn",
      sql);
    assertEquals("number of parameters" + parameters, 3, parameters.size());
    assertEquals("^(knitting)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(needle)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    assertEquals(Integer.valueOf(1), parameters.get(2));
    assertTrue(parameters.get(2) instanceof Integer);
    
    assertEquals("Description", "_^(knitting)$_^(needle)$", search.getDescription());

  }
  
  /**
   * Ensure one-column segment searches (with and without word matching)
   * generate the correct SQL.
   */   
  @Test public void segmentSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    // with a word-layer condition
    search.setMatrix(
      new Matrix().addColumn(
        new Column()
        .addLayerMatch(new LayerMatch()
                       .setId("orthography").setPattern("kit"))
        .addLayerMatch(new LayerMatch()
                       .setId("segment").setPattern("I").setTarget(true))
        ));
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "with word layer condition",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, search_0_2.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, search_0_2.start_anchor_id,"
      +" search_0_2.end_anchor_id,0, search_0_1.segment_annotation_id AS segment_annotation_id,"
      +" search_0_1.annotation_id AS target_annotation_id,"
      +" search_0_2.turn_annotation_id AS turn_annotation_id,"
      +" search_0_2.word_annotation_id AS first_matched_word_annotation_id,"
      +" search_0_2.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('es_1_', search_0_1.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_1 segment_0"
      +" ON segment_0.turn_annotation_id = turn.annotation_id"
      +" INNER JOIN anchor segment_0_start"
      +" ON segment_0_start.anchor_id = segment_0.start_anchor_id"
      +" INNER JOIN (annotation_layer_2 search_0_2"
      +"  INNER JOIN anchor word_0_start_2"
      +"  ON word_0_start_2.anchor_id = search_0_2.start_anchor_id"
      +"  INNER JOIN anchor word_0_end_2  ON word_0_end_2.anchor_id = search_0_2.end_anchor_id)"
      +"  ON search_0_2.word_annotation_id = segment_0.word_annotation_id"
      +"  AND word_0_start_2.offset <= segment_0_start.offset"
      +"  AND word_0_end_2.offset > segment_0_start.offset"
      +"  AND search_0_2.label  REGEXP  ?"
      +"  INNER JOIN annotation_layer_1 search_0_1"
      +"  ON search_0_1.segment_annotation_id = segment_0.annotation_id"
      +"  AND CAST(search_0_1.label AS BINARY)  REGEXP BINARY ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY search_0_2.turn_annotation_id, search_0_2.ordinal_in_turn,"
      +" search_0_1.ordinal_in_word",
      sql);
    assertEquals("number of parameters" + parameters, 2, parameters.size());
    assertEquals("^(kit)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    assertEquals("^(I)$", parameters.get(1));
    assertTrue(parameters.get(1) instanceof String);
    
    assertEquals("Description", "_^(kit)$_^(I)$", search.getDescription());

    // only segment layer
    search.setMatrix(
      new Matrix().addColumn(
        new Column()
        .addLayerMatch(new LayerMatch()
                       .setId("segment").setPattern("I").setTarget(true))
        ));
    parameters.clear();
    sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "only segment layer",
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id,"
      +" first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, word_0.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, word_0.start_anchor_id,"
      +" word_0.end_anchor_id,0, search_0_1.segment_annotation_id AS segment_annotation_id,"
      +" search_0_1.annotation_id AS target_annotation_id,"
      +" word_0.turn_annotation_id AS turn_annotation_id,"
      +" word_0.word_annotation_id AS first_matched_word_annotation_id,"
      +" word_0.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('es_1_', search_0_1.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_0 word_0 ON word_0.turn_annotation_id = turn.annotation_id"
      +" INNER JOIN annotation_layer_1 segment_0"
      +" ON segment_0.word_annotation_id = word_0.annotation_id"
      +" INNER JOIN anchor segment_0_start"
      +" ON segment_0_start.anchor_id = segment_0.start_anchor_id"
      +" INNER JOIN annotation_layer_1 search_0_1"
      +"  ON search_0_1.segment_annotation_id = segment_0.annotation_id"
      +"  AND CAST(search_0_1.label AS BINARY)  REGEXP BINARY ?"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY word_0.turn_annotation_id, word_0.ordinal_in_turn, search_0_1.ordinal_in_word",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(I)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(I)$", search.getDescription());
  }

  /**
   * Ensure segment search with alignment confidence threshold generates the correct SQL.
   */   
  @Test public void alignedSegmentSearch() throws Exception {    
    OneQuerySearch search = new OneQuerySearch();
    // with a word-layer condition
    search.setMatrix(
      new Matrix().addColumn(
        new Column()
        .addLayerMatch(new LayerMatch()
                       .setId("segment").setPattern("I").setTarget(true))
        ));
    search.setAnchorConfidenceThreshold((byte)50);
    Vector<Object> parameters = new Vector<Object>();
    String sql = search.generateSql(parameters, getSchema(), l -> false, p -> "", t -> "");
    assertEquals(
      "INSERT INTO _result"
      +" (search_id, ag_id, speaker_number, start_anchor_id, end_anchor_id,"
      +" defining_annotation_id, segment_annotation_id, target_annotation_id, turn_annotation_id, first_matched_word_annotation_id, last_matched_word_annotation_id, complete,"
      +" target_annotation_uid) SELECT ?, word_0.ag_id AS ag_id,"
      +" CAST(turn.label AS SIGNED) AS speaker_number, word_0.start_anchor_id,"
      +" word_0.end_anchor_id,0, search_0_1.segment_annotation_id AS segment_annotation_id,"
      +" search_0_1.annotation_id AS target_annotation_id,"
      +" word_0.turn_annotation_id AS turn_annotation_id,"
      +" word_0.word_annotation_id AS first_matched_word_annotation_id,"
      +" word_0.word_annotation_id AS last_matched_word_annotation_id, 0 AS complete,"
      +" CONCAT('es_1_', search_0_1.annotation_id) AS target_annotation_uid"
      +" FROM annotation_layer_11 turn"
      +" /* extra joins */"
      +"  INNER JOIN annotation_layer_0 word_0 ON word_0.turn_annotation_id = turn.annotation_id"
      +" INNER JOIN annotation_layer_1 segment_0"
      +" ON segment_0.word_annotation_id = word_0.annotation_id"
      +" INNER JOIN anchor segment_0_start"
      +" ON segment_0_start.anchor_id = segment_0.start_anchor_id"
      +" INNER JOIN annotation_layer_1 search_0_1"
      +"  ON search_0_1.segment_annotation_id = segment_0.annotation_id"
      +"  AND CAST(search_0_1.label AS BINARY)  REGEXP BINARY ?"
      +" INNER JOIN anchor word_0_start"
      +" ON word_0_start.anchor_id = word_0.start_anchor_id"
      +" AND word_0_start.alignment_status >= 50"
      +" INNER JOIN anchor word_0_end"
      +" ON word_0_end.anchor_id = word_0.end_anchor_id"
      +" AND word_0_end.alignment_status >= 50"
      +" /* subsequent columns */"
      +"  WHERE 1=1"
      +" /* transcripts */"
      +"  /* participants */"
      +"  /* main participant clause */"
      +"  /* access clause */"
      +"  /* first column: */"
      +" /* border conditions */"
      +"  /* search criteria subqueries */"
      +"  /* subsequent columns */"
      +"  ORDER BY word_0.turn_annotation_id, word_0.ordinal_in_turn, search_0_1.ordinal_in_word",
      sql);
    assertEquals("number of parameters" + parameters, 1, parameters.size());
    assertEquals("^(I)$", parameters.get(0));
    assertTrue(parameters.get(0) instanceof String);
    
    assertEquals("Description", "_^(I)$", search.getDescription());

  }

  /**
   * Return a plausible schema, including SQL attributes.
   * @return A test schema.
   */
  public Schema getSchema() {
    return new Schema(
      "who", "turn", "utterance", "word",
      
      (Layer)(new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("class_id", "transcript").with("attribute", "language"),
      
      (Layer)(new Layer("transcript_scribe", "Scribe").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("class_id", "transcript").with("attribute", "scribe"),
      
      new Layer("transcript_type", "Type").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      
      (Layer)(new Layer("transcript_rating", "Ratings").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(true).setPeersOverlap(true).setSaturated(true))
      .with("class_id", "transcript").with("attribute", "rating"),
      
      (Layer)(new Layer("corpus", "Corpus").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("layer_id", SqlConstants.LAYER_CORPUS),
      
      (Layer)(new Layer("episode", "Episode").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("layer_id", SqlConstants.LAYER_SERIES),
      
      (Layer)(new Layer("recording_date", "Recording Date").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true).setParentId("episode")),
      
      (Layer)(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(true).setPeersOverlap(true).setSaturated(true))
      .with("layer_id", SqlConstants.LAYER_PARTICIPANT),
      
      (Layer)(new Layer("main_participant", "Main Participant").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(true).setPeersOverlap(true).setSaturated(true).setParentId("who"))
      .with("layer_id", SqlConstants.LAYER_MAIN_PARTICIPANT),
      
      (Layer)(new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(true).setPeersOverlap(true).setSaturated(true).setParentId("who"))
      .with("class_id", "speaker").with("attribute", "gender"),
      
      (Layer)(new Layer("participant_age", "Age").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(true).setPeersOverlap(true).setSaturated(true).setParentId("who"))
      .with("class_id", "speaker").with("attribute", "age"),
      
      (Layer)(new Layer("comment", "Comment").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(false))
      .with("layer_id", 31).with("scope", "F"),
      
      (Layer)(new Layer("noise", "Noise")
              .setAlignment(2).setPeers(true).setPeersOverlap(false).setSaturated(false))
      .with("layer_id", 32).with("scope", "F"),
      
      (Layer)(new Layer("topic", "Topic").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(false))
      .with("layer_id", 30).with("scope", "F"),
      
      (Layer)(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(false)
              .setParentId("who").setParentIncludes(true))
      .with("layer_id", SqlConstants.LAYER_TURN).with("scope", "M"),
      
      (Layer)(new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(true)
              .setParentId("turn").setParentIncludes(true))
      .with("layer_id", SqlConstants.LAYER_UTTERANCE).with("scope", "M"),
      
      (Layer)(new Layer("language", "Other Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(false)
              .setParentId("turn").setParentIncludes(true))
      .with("layer_id", 20).with("scope", "M"),
      
      (Layer)(new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(false)
              .setParentId("turn").setParentIncludes(true))
      .with("layer_id", SqlConstants.LAYER_TRANSCRIPTION).with("scope", "W"),
      
      (Layer)(new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true)
              .setParentId("word").setParentIncludes(true))
      .with("layer_id", SqlConstants.LAYER_ORTHOGRAPHY).with("scope", "W"),
      
      (Layer)(new Layer("phonemes", "Pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(true).setPeersOverlap(false).setSaturated(true)
              .setParentId("word").setParentIncludes(true)
              .setType(Constants.TYPE_IPA))
      .with("layer_id", 52).with("scope", "W"),
      
      (Layer)(new Layer("syllableCount", "Syllable Count").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true)
              .setParentId("word").setParentIncludes(true)
              .setType(Constants.TYPE_NUMBER))
      .with("layer_id", 186).with("scope", "W"),
      
      (Layer)(new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
              .setPeers(true).setPeersOverlap(false).setSaturated(true)
              .setParentId("word").setParentIncludes(true)
              .setType(Constants.TYPE_IPA))
      .with("layer_id", SqlConstants.LAYER_SEGMENT).with("scope", "S"),
      
      (Layer)(new Layer("pronounce", "Pronounce").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true)
              .setParentId("word").setParentIncludes(true))
      .with("layer_id", 23).with("scope", "W"))

      .setEpisodeLayerId("episode")
      .setCorpusLayerId("corpus");
  } // end of getSchema()

  public static void main(String args[])  {
    org.junit.runner.JUnitCore.main("nzilbb.labbcat.server.db.test.TestOneQuerySearch");
  }

}
