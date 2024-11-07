//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
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

package nzilbb.labbcat.server.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nzilbb.ag.Schema;
import nzilbb.labbcat.server.db.OneQuerySearch;
import nzilbb.labbcat.server.db.SqlGraphStore;
import nzilbb.labbcat.server.db.SqlGraphStoreAdministration;
import nzilbb.labbcat.server.db.StoreCache;
import nzilbb.labbcat.server.search.Column;
import nzilbb.labbcat.server.search.Matrix;

/**
 * <tt>/api/search</tt>
 * : Starts a search task to identify matches for the given search matrix.
 * <p> The search matrix defines a pattern of annotations to identify, which transcripts
 * to search, and which participant's utterances to target. Additional search parameters
 * can also be defined; whether to return only aligned matches, whether main-participants
 * only are being targeted, etc.
 * <p><b>Input HTTP parameters</b>:
 * <ul>
 *  <li><i>searchJson</i> - A JSON-encoded reprentation of the search matrix. </li>
 *  <li><i>search</i> - An alternative to <i>searchJson</i>, this encodes an
        orthography-layer-only search as a plain-text string. </li>
 *  <li><i>mainParticipantOnly</i> - Optional: specify a value (e.g. "true") if only
 *      main-participant utterances should be searched, absent otherwise. </li>
 *  <li><i>offsetThreshold</i> - Optional minimum alignment confidence for matching word or
 *      segment annotations. A value of 50 means that annotations that were at least
 *      automatically aligned will be returned. Use 100 for manually-aligned annotations
 *      only, and 0 or no value to return all matching annotations regardless of alignment
 *      confidence.</li> 
 *  <li><i>matchesPerTranscript</i> - Optional maximum number of matches per transcript
 *      to return, or absent to return all matches in each transcript.</li>
 *  <li><i>overlapThreshold</i> - Optional (integer) percentage overlap with other
 *      utterances before simultaneous speech is excluded, or absent to include
 *      overlapping speech.</li>  
 *  <li><i>maxMatches</i> - Optional maximum number of transcripts to include results for.</li>
 *  <li><i>suppressResults</i> - Optional: Specify a value (e.g. "true") to return a
 *      summary of results only, instead of listing specific matches.</li>
 *  <li><i>participantQuery</i> - Optional AGQL expression for defining which
 *      participants to search the utterances of,
 *      e.g. <q>first('participant_gender').label == 'NB'</q></li>
 *  <li><i>participant_expression</i> - Deprecated synonym for <i>participantQuery</i>.</li>
 *  <li><i>transcriptQuery</i> - Optional AGQL expression for defining which
 *      transcript to search,
 *      e.g. <q>['CC','IA'].includesAny(labels('corpus'))</q></li>
 *  <li><i>transcript_expression</i> - Deprecated synonymn for <i>transcriptQuery</i>.</li>
 *  <li><i>only_main_speaker</i> - Deprecated synonym for <i>mainParticipantOnly</i>.</li>
 *  <li><i>matches_per_transcript</i> - Deprecated synonym for <i>matchesPerTranscript</i>.</li>
 *  <li><i>overlap_threshold</i> - Deprecated synonym for <i>overlapThreshold</i>.</li>  
 *  <li><i>num_transcripts</i> - Deprecated synonym for <i>maxMatches</i>.</li>
 *  <li><i>suppress_results</i> - Deprecated synonym for <i>suppressResults</i>.</li>
 *  <li><i>only_aligned</i> - Deprecated: specifying a value (e.g. "true") is equivalent
 *      to specifying <a>offsetThreshold</a> with a value of "50".</li> 
 * </ul>
 * <br><b>Output</b>: a JSON-encoded response object of the usual structure for which the
 * "model" is an object with a "threadId" attribute, which is the ID of the server task to
 * monitor for results. e.g. 
 * <pre>{
 *    "title":"search",
 *    "version" : "20230502.0924",
 *    "code" : 0,
 *    "errors" : [],
 *    "messages" : [],
 *    "model" : {
 *        "threadId" : "80"
 *    }
 * }</pre>
 * <br> The task, when finished, will output a URL for accessing the matches of the search.
 * @author Robert Fromont
 */
@WebServlet({"/api/search"} )
public class Search extends LabbcatServlet {
  
  /**
   * Constructor
   */
  public Search() {
  } // end of constructor
  
  // Servlet methods
  
  /**
   * The POST method for the servlet.
   * <p> This expects a multipart request body with parameters as defined above.
   * @param request HTTP request
   * @param response HTTP response
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // parameters
    Matrix matrix = new Matrix();
    String searchJson = request.getParameter("searchJson");
    String search = request.getParameter("search");
    if (searchJson != null) {
      matrix.fromJsonString(searchJson);
    } else if (search != null) {
      matrix.fromLegacyString(search);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeResponse(response, failureResult(request, "No search matrix specified."));
      return;
    }
    if (request.getParameter("participantQuery") != null) {
      matrix.setParticipantQuery(request.getParameter("participantQuery"));
    } else if (request.getParameter("participant_expression") != null) {
      matrix.setParticipantQuery(request.getParameter("participant_expression"));
    } else if (request.getParameter("participant_id") != null) {
      matrix.setParticipantQuery(
        "id IN ("
        +Arrays.stream(request.getParameterValues("participant_id"))
        .map(id->"'"+id+"'")
        .collect(Collectors.joining(","))
        +")");
    }
    if (request.getParameter("transcriptQuery") != null) {
      matrix.setTranscriptQuery(request.getParameter("transcriptQuery"));
    } else if (request.getParameter("transcript_expression") != null) {
      matrix.setTranscriptQuery(request.getParameter("transcript_expression"));
    }
    OneQuerySearch task = new OneQuerySearch();
    task.setMatrix(matrix);
    if (request.getParameter("mainParticipantOnly") != null
        || request.getParameter("only_main_speaker") != null) {
      task.setMainParticipantOnly(true);
    }
    if (request.getParameter("suppressResults") != null
        || request.getParameter("suppress_results") != null) {
      task.setSuppressResults(true);
    }
    String offsetThreshold = request.getParameter("offsetThreshold");
    if (offsetThreshold == null && request.getParameter("only_aligned") != null) {
      offsetThreshold = "50";
    }
    if ("0".equals(offsetThreshold)) offsetThreshold = null;
    if (offsetThreshold != null) {
      try {
        task.setAnchorConfidenceThreshold(Byte.valueOf(offsetThreshold));
      } catch(NumberFormatException exception) {
        writeResponse(response, failureResult(
                        request, "Invalid offset threshold \"{0}\": {1}",
                        offsetThreshold, exception.getMessage()));
        return;
      }
    }
    String matchesPerTranscript = request.getParameter("matchesPerTranscript");
    if (matchesPerTranscript == null) {
      matchesPerTranscript = request.getParameter("matches_per_transcript");
    }
    if (matchesPerTranscript != null) {
      try {
        task.setMatchesPerTranscript(Integer.valueOf(matchesPerTranscript));
      } catch(NumberFormatException exception) {
        writeResponse(response, failureResult(
                        request, "Invalid matches per transcript \"{0}\": {1}",
                        matchesPerTranscript,
                        exception.getMessage()));
        return;
      }
    }
    String overlapThreshold = request.getParameter("overlapThreshold");
    if (overlapThreshold == null) overlapThreshold = request.getParameter("overlap_threshold");
    if (overlapThreshold != null) {
      try {
        task.setOverlapThreshold(Integer.valueOf(overlapThreshold));
      } catch(NumberFormatException exception) {
        writeResponse(response, failureResult(
                        request, "Invalid overlap threshold \"{0}\": {1}",
                        overlapThreshold, exception.getMessage()));
        return;
      }
    }
    String maxMatches = request.getParameter("maxMatches");
    if (maxMatches == null) maxMatches = request.getParameter("num_transcripts");
    if (maxMatches != null) {
      try {
        task.setMaxMatches(Integer.valueOf(maxMatches));
      } catch(NumberFormatException exception) {
        writeResponse(response, failureResult(
                        request, "Invalid maximum number of matches \"{0}\": {1}",
                        maxMatches, exception.getMessage()));
        return;
      }
    }
    
    try {
      final SqlGraphStoreAdministration store = getStore(request);
      task.setStoreCache(new StoreCache() {
          public SqlGraphStore get() {
            return store;
          }
          public void accept(SqlGraphStore store) {
            cacheStore((SqlGraphStoreAdministration)store);
          }
        });
      if (request.getRemoteUser() != null) {	
        task.setWho(request.getRemoteUser());
        // admin users have access to everything
        if (!IsUserInRole("admin", request, task.getStore().getConnection())
            // if they're using using access permissions in general
            && task.getStore().getPermissionsSpecified()) {
          // other users may have restricted access to some things
          task.setRestrictByUser(request.getRemoteUser());
        } // not an admin user
      } else {
        task.setWho(request.getRemoteHost());
      }
      
      String validationError = task.validate();
      if (validationError != null) {
        writeResponse(response, failureResult(request, validationError));
      } else {
        task.start();
        
        // return its ID
        JsonObjectBuilder jsonResult = Json.createObjectBuilder()
          .add("threadId", ""+task.getId());
        writeResponse(
          response, successResult(request, jsonResult.build(), null));
      } // valid search
    } catch(Exception ex) {
      throw new ServletException(ex);
    }
  }
  
  private static final long serialVersionUID = -1;
} // end of class Search
