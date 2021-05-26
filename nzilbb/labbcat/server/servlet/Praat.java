//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.labbcat.server.servlet;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.*;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.*; // d:/jakarta-tomcat-5.0.28/common/lib/servlet-api.jar
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphStoreAdministration;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.ConfigurationHelper;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.ParameterSet;
import nzilbb.labbcat.server.db.SqlGraphStoreAdministration;
import nzilbb.labbcat.server.task.ProcessWithPraat;
import nzilbb.util.IO;
import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * <tt>/api/praat</tt>
 * : Processes a given set of audio intervals with Praat.
 * <p> A given CSV file, with given column indices identifying the transcript,
 * participant, start, and end columns, is traversed, and each row is processed with Praat
 * to extract given acoustic measurements.
 * <p> The request method must be <b> POST </b>
 * <p> The multipart-encoded parameters are:
 *  <dl>
 *   <dt> csv </dt>
 *       <dd> CSV results file containing tokens to measure. </dd>
 *   <dt> transcriptColumn </dt>
 *       <dd> CSV column index of the transcript name.</dd>
 *   <dt> participantColumn </dt>
 *       <dd> CSV column index of the participant name.</dd>
 *   <dt> startTimeColumn </dt>
 *       <dd> CSV column index of the start time.</dd>
 *   <dt> endTimeColumn </dt>
 *       <dd> CSV column index of the end time name.</dd>
 *   <dt> windowOffset </dt>
 *       <dd> How much surrounsing context to include, in seconds.</dd>
 *   <dt> passThroughData </dt>
 *       <dd> Whether to include all CSV columns from the input file in the output file -
 *            "false" if not.</dd> 
 *   <dt> extractF1 </dt>
 *       <dd> Extract F1.</dd>
 *   <dt> extractF2 </dt>
 *       <dd> Extract F2.</dd>
 *   <dt> extractF3 </dt>
 *       <dd> Extract F3.</dd>
 *   <dt> samplePoints </dt>
 *       <dd> Space-delimited series of real numbers between 0 and 1, specifying the
 *            proportional time points to measure formants. e.g. "0.5" will measure only
 *            the mid-point, "0 0.2 0.4 0.6 0.8 1" will measure six points evenly spread
 *            across the duration of the segment, etc.</dd> 
 *   <dt> formantCeilingDefault </dt>
 *       <dd> Value to use as the formant ceiling by default</dd>
 *   <dt> formantDifferentiationLayerId </dt>
 *       <dd> Participant attribute
 *            layer ID for differentiating formant settings; this will typically be
 *            "participant_gender" but can be any participant attribute layer. </dd> 
 *   <dt> formantOtherPattern (multiple values) </dt>
 *       <dd> Array of regular expression strings to match against the value of that
 *            attribute identified by <var>formantDifferentiationLayerId</var>. If the
 *            participant's attribute value matches the pattern for an element in this
 *            array, the corresponding element in <var>formantCeilingOther</var> will be
 *            used for that participant. </dd> 
 *   <dt> formantCeilingOther (multiple values) </dt>
 *       <dd> Values to use as the formant ceiling for participants who's attribute value
 *            matches the corresponding regular expression in <var>formantOtherPattern</var></dd>
 *   <dt> scriptFormant </dt>
 *       <dd> Formant extraction script command.
 *   (default: "To Formant (burg)... 0.0025 5 formantCeiling 0.025 50") </dd>
 * 
 *   <dt> useFastTrack ("true" or "false") </dt>
 *       <dd> Use the FastTrack plugin to generate optimum, smoothed formant
 *       tracks. (default: false)</dd>
 *   <dt> fastTrackTimeStep </dt>
 *       <dd> Fast Track time_step global setting. </dd>
 *   <dt> fastTrackBasisFunctions </dt>
 *       <dd> Fast Track basis_functions global setting - "dct". </dd>
 *   <dt> fastTrackErrorMethod </dt>
 *       <dd> Fast Track error_method global setting - "mae". </dd>
 *   <dt> fastTrackTrackingMethod </dt>
 *       <dd> Fast Track tracking_method parameter for trackAutoselectProcedure; "burg" or
 *       "robust". </dd> 
 *   <dt> fastTrackEnableF1FrequencyHeuristic ("true" or "false") </dt>
 *       <dd> Fast Track enable_F1_frequency_heuristic global setting. </dd>
 *   <dt> fastTrackMaximumF1FrequencyValue </dt>
 *       <dd> Fast Track maximum_F1_frequency_value global setting. </dd>
 *   <dt> fastTrackEnableF1BandwidthHeuristic </dt>
 *       <dd> Fast Track enable_F1_bandwidth_heuristic global setting. </dd>
 *   <dt> fastTrackMaximumF1BandwidthValue </dt>
 *       <dd> Fast Track maximum_F1_bandwidth_value global setting. </dd>
 *   <dt> fastTrackEnableF2BandwidthHeuristic ("true" or "false") </dt>
 *       <dd> Fast Track enable_F2_bandwidth_heuristic global setting. </dd>
 *   <dt> fastTrackMaximumF2BandwidthValue </dt>
 *       <dd> Fast Track maximum_F2_bandwidth_value global setting. </dd>
 *   <dt> fastTrackEnableF3BandwidthHeuristic ("true" or "false") </dt>
 *       <dd> Fast Track enable_F3_bandwidth_heuristic global setting.. </dd>
 *   <dt> fastTrackMaximumF3BandwidthValue </dt>
 *       <dd> Fast Track maximum_F3_bandwidth_value global setting. </dd>
 *   <dt> fastTrackEnableF4FrequencyHeuristic ("true" or "false") </dt>
 *       <dd> Fast Track enable_F4_frequency_heuristic global setting. </dd>
 *   <dt> fastTrackMinimumF4FrequencyValue </dt>
 *       <dd> Fast Track minimum_F4_frequency_value global setting. </dd>
 *   <dt> fastTrackEnableRhoticHeuristic ("true" of "false") </dt>
 *       <dd> Fast Track enable_rhotic_heuristic global setting. </dd>
 *   <dt> fastTrackEnableF3F4ProximityHeuristic </dt>
 *       <dd> Fast Track enable_F3F4_proximity_heuristic global setting. </dd>
 *   <dt> fastTrackNumberOfSteps </dt>
 *       <dd> Fast Track number of steps. </dd>
 *   <dt> fastTrackNumberOfCoefficients </dt>
 *       <dd> Fast Track number of coefficients for the regression function. </dd>
 *   <dt> fastTrackNumberOfFormants </dt>
 *       <dd> Fast Track number of formants. </dd>
 *   <dt> fastTrackCoefficients ("true" or "false") </dt>
 *       <dd> Whether to return the regression coefficients from FastTrack. </dd>
 * 
 *   <dt> extractMinimumPitch ("true" or "false") </dt>
 *       <dd> Extract minimum pitch. (default: false) </dd>
 *   <dt> extractMeanPitch ("true" or "false") </dt>
 *       <dd> Extract mean pitch. (default: false) </dd>
 *   <dt> extractMaximumPitch ("true" or "false") </dt>
 *       <dd> Extract maximum pitch. (default: false) </dd>
 *   <dt> pitchFloorDefault (int) </dt>
 *       <dd> Pitch Floor by default. (default: 60) </dd>
 *   <dt> pitchCeilingDefault (int) </dt>
 *       <dd> Pitch Ceiling by default. (default: 500) </dd>
 *   <dt> voicingThresholdDefault (int) </dt>
 *       <dd> Voicing Threshold by default. (default: 0.5) </dd>
 *   <dt> pitchDifferentiationLayerId (string) </dt>
 *       <dd> Participant attribute layer ID for differentiating pitch settings; this will
 *            typically be "participant_gender" but can be any participant attribute layer. </dd>
 *   <dt> pitchOtherPattern (string[]) </dt>
 *       <dd> Array of regular expression strings to match against the value of that
 *       attribute identified by <var>pitchDifferentiationLayerId</var>. If the
 *       participant's attribute value matches the pattern for an element in this array,
 *       the corresponding element in <var>pitchFloorOther</var>,
 *       <var>pitchCeilingOther</var>, and <var>voicingThresholdOther</var> will be used
 *       for that participant. </dd>  
 *   <dt> pitchFloorOther (int[]) </dt>
 *       <dd> Values to use as the pitch floor for participants who's attribute value
 *       matches the corresponding regular expression in <var>pitchOtherPattern</var></dd>
 *   <dt> pitchCeilingOther (int[]) </dt>
 *       <dd> Values to use as the pitch ceiling for participants who's attribute value
 *       matches the corresponding regular expression in <var>pitchOtherPattern</var></dd>
 *   <dt> voicingThresholdOther (int[]) </dt>
 *       <dd> Values to use as the voicing threshold for participants who's attribute
 *       value matches the corresponding regular expression in <var>pitchOtherPattern</var></dd>
 *   <dt> scriptPitch (string) </dt>
 *       <dd> Pitch extraction script command. (default: 
 * "To Pitch (ac)...  0 pitchFloor 15 no 0.03 voicingThreshold 0.01 0.35 0.14 pitchCeiling") </dd>
 * 
 *   <dt> extractMaximumIntensity ("true" or "false") </dt>
 *       <dd> Extract maximum intensity.  (default: false) </dd>
 *   <dt> intensityPitchFloorDefault </dt>
 *       <dd> Pitch Floor by default. (default: 60) </dd>
 *   <dt> intensityDifferentiationLayerId </dt>
 *       <dd> Participant attribute layer ID for differentiating intensity settings; this
 *       will typically be "participant_gender" but can be any participant attribute layer. </dd>
 *   <dt> intensityOtherPattern (multiple values) </dt>
 *       <dd> Array of regular expression strings to match against the value of that
 *       attribute identified by <var>intensityDifferentiationLayerId</var>. If the
 *       participant's attribute value matches the pattern for an element in this array,
 *       the corresponding element in <var>intensityPitchFloorOther</var> will be used for
 *       that participant. </dd>  
 *   <dt> intensityPitchFloorOther (multiple values) </dt>
 *       <dd> Values to use as the pitch floor for participants who's attribute value
 *       matches the corresponding regular expression in
 *       <var>intensityPitchOtherPattern</var></dd> 
 *   <dt> scriptIntensity </dt>
 *       <dd> Pitch extraction script command. 
 *       (default: "To Intensity... intensityPitchFloor 0 yes") </dd>
 * 
 *   <dt> extractCOG1 ("true" or "false") </dt>
 *       <dd> Extract COG 1. (default: false) </dd>
 *   <dt> extractCOG2 ("true" or "false") </dt>
 *       <dd> Extract COG 2. (default: false) </dd>
 *   <dt> extractCOG23 ("true" or "false") </dt>
 *       <dd> Extract COG 2/3. (default: false) </dd>
 *
 *   <dt> script </dt>
 *       <dd> A user-specified custom Praat script to execute on each segment. </dd> 
 *   <dt> attributes (multiple values) </dt>
 *       <dd> A list of participant attribute layer IDs to include as variables for the
 *       custom Praat <var>script</var>. </dd>  
 *  </dl>
 * <p><b>Output</b>: A JSON-encoded response containing the threadId of a task that is
 * processing the request. The task, when finished, will output a CSV files with one line
 * for each line of the input file, and fields containing the selected acoustic
 * measurements. If <code>passThroughData</code> is "false", the output will
 * <em>not</em> contain the data from the input file, otherwise the input file data will
 * be passed through into the output file, with the acoustic measurement fields appended
 * to each line. 
 * @author Robert Fromont
 */
@WebServlet({"/api/praat"} )
public class Praat extends LabbcatServlet { // TODO unit test
   
  // Attributes:
     
  /**
   * Constructor
   */
  public Praat() {
  } // end of constructor
  
  // Servlet methods
  
  /**
   * The POST method for the servlet.
   * <p> This expects a multipart request body with parameters as defined above.
   * @param request HTTP request
   * @param response HTTP response
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
      try {
        SqlGraphStoreAdministration store = getStore(request);
        try {
          // interpret request parameters
          MultipartRequestParameters parameters = new MultipartRequestParameters(request); 
          Vector<FileItem> files =  parameters.getFiles("csv");
          if (files.size() == 0) {
            writeResponse(response, failureResult(request, "No file received."));
            return;
          }
          // save the input file
          FileItem csv = files.elementAt(0);
          String fileName = csv.getName();
          // some browsers provide a full path, which must be truncated
          int lastSlash = fileName.lastIndexOf('/');
          if (lastSlash < 0) lastSlash = fileName.lastIndexOf('\\');
          if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
          }
          // save the file
          File uploadedCsvFile = File.createTempFile("Praat-", "-"+fileName);
          uploadedCsvFile.delete();
          uploadedCsvFile.deleteOnExit();
          csv.write(uploadedCsvFile);        
          
          ProcessWithPraat task = new ProcessWithPraat();
          task.setStore(store); // TODO check this doesn't leak!
          task.setDataFile(uploadedCsvFile);
          task.setFileName(fileName);
          
          if (parameters.getString("transcriptColumn") != null) {
            try {
              task.setTranscriptIdColumn(
                Integer.parseInt(parameters.getString("transcriptColumn")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "Transcript column \""+parameters.getString("transcriptColumn")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          } else {
            writeResponse(
              response, failureResult(
                request, "Transcript column not supplied.")); // TODO i18n
          }
          
          if (parameters.getString("participantColumn") != null) {
            try {
              task.setParticipantNameColumn(
                Integer.parseInt(parameters.getString("participantColumn")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "Participant column \""+parameters.getString("participantColumn")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          } else {
            writeResponse(
              response, failureResult(
                request, "Participant column not supplied.")); // TODO i18n
          }
          
          if (parameters.getString("startTimeColumn") != null) {
            try {
              task.setMarkColumn(Integer.parseInt(parameters.getString("startTimeColumn")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "Start time column \""+parameters.getString("startTimeColumn")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          } else {
            writeResponse(
              response, failureResult(
                request, "Start time column not supplied.")); // TODO i18n
          }
          
          if (parameters.getString("endTimeColumn") != null) {
            try {
              task.setMarkEndColumn(Integer.parseInt(parameters.getString("endTimeColumn")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "End time column \""+parameters.getString("endTimeColumn")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          } else {
            writeResponse(
              response, failureResult(
                request, "End time column not supplied.")); // TODO i18n
          }
          
          if (parameters.getString("windowOffset") != null) {
            try {
              task.setWindowOffset(Double.parseDouble(parameters.getString("windowOffset")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "Window offset \""+parameters.getString("windowOffset")
                  +"\" is not a number.")); // TODO i18n
              return;
            }
          }
          
          if (parameters.getString("passThroughData") != null
              && parameters.getString("passThroughData").equalsIgnoreCase("false")) {
            task.setPassThroughData(false);
          }

          task.setExtractF1("true".equalsIgnoreCase(parameters.getString("extractF1")));
          task.setExtractF2("true".equalsIgnoreCase(parameters.getString("extractF2")));
          task.setExtractF3("true".equalsIgnoreCase(parameters.getString("extractF3")));

          if (parameters.getString("samplePoints") != null) {
            StringTokenizer tokens = new StringTokenizer(
              parameters.getString("samplePoints"), " ,;:-");
            task.getSamplePoints().clear();
            while (tokens.hasMoreTokens()) {
              task.getSamplePoints().add(Double.valueOf(tokens.nextToken()));
            }            
          }
          
          if (parameters.getString("formantDifferentiationLayerId") != null) {
            task.setFormantDifferentiationLayerId(
              parameters.getString("formantDifferentiationLayerId"));
          }
          task.getFormantOtherPattern().clear();
          for (String value : parameters.getStrings("formantOtherPattern")) {
            try {
              task.getFormantOtherPattern().add(Pattern.compile(value));
            } catch(PatternSyntaxException exception) {
              writeResponse(
                response, failureResult(
                  request, "formantOtherPattern \""+value
                  +"\" is not a valid regular expression: "
                  + exception.getMessage())); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("formantCeilingDefault") != null) {
            try {
              task.setFormantCeilingDefault(
                Integer.parseInt(parameters.getString("formantCeilingDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request,
                  "formantCeilingDefault \""+parameters.getString("formantCeilingDefault")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.getFormantCeilingOther().clear();
          for (String value : parameters.getStrings("formantCeilingOther")) {
            try {
              task.getFormantCeilingOther().add(Integer.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "formantCeilingOther \""+value+"\" is not an integer.")); // TODO i18n
              return;
            }
          } // next value          
          if (parameters.getString("scriptFormant") != null
              && parameters.getString("scriptFormant").length() > 0) {
            task.setScriptFormant(parameters.getString("scriptFormant"));
          }

          task.setUseFastTrack(
            "true".equalsIgnoreCase(parameters.getString("useFastTrack")));
          if (parameters.getString("fastTrackDifferentiationLayerId") != null) {
            task.setFastTrackDifferentiationLayerId(
              parameters.getString("fastTrackDifferentiationLayerId"));
          }
          task.getFastTrackOtherPattern().clear();
          for (String value : parameters.getStrings("fastTrackOtherPattern")) {
            try {
              task.getFastTrackOtherPattern().add(Pattern.compile(value));
            } catch(PatternSyntaxException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackOtherPattern \""+value
                  +"\" is not a valid regular expression: "
                  + exception.getMessage())); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("fastTrackLowestAnalysisFrequencyDefault") != null) {
            try {
              task.setFastTrackLowestAnalysisFrequencyDefault(
                Integer.parseInt(parameters.getString("fastTrackLowestAnalysisFrequencyDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request,
                  "fastTrackLowestAnalysisFrequencyDefault \""
                  +parameters.getString("fastTrackLowestAnalysisFrequencyDefault")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.getFastTrackLowestAnalysisFrequencyOther().clear();
          for (String value : parameters.getStrings("fastTrackLowestAnalysisFrequencyOther")) {
            try {
              task.getFastTrackLowestAnalysisFrequencyOther().add(Integer.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackLowestAnalysisFrequencyOther \""
                  +value+"\" is not an integer.")); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("fastTrackHighestAnalysisFrequencyDefault") != null) {
            try {
              task.setFastTrackHighestAnalysisFrequencyDefault(
                Integer.parseInt(parameters.getString("fastTrackHighestAnalysisFrequencyDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request,
                  "fastTrackHighestAnalysisFrequencyDefault \""
                  +parameters.getString("fastTrackHighestAnalysisFrequencyDefault")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.getFastTrackHighestAnalysisFrequencyOther().clear();
          for (String value : parameters.getStrings("fastTrackHighestAnalysisFrequencyOther")) {
            try {
              task.getFastTrackHighestAnalysisFrequencyOther().add(Integer.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackHighestAnalysisFrequencyOther \""
                  +value+"\" is not an integer.")); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("fastTrackTimeStep") != null) {
            try {
              task.setFastTrackTimeStep(
                Double.parseDouble(parameters.getString("fastTrackTimeStep")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request,
                  "fastTrackTimeStep \""+parameters.getString("fastTrackTimeStep")
                  +"\" is not a number.")); // TODO i18n
              return;
            }
          }
          if (parameters.getString("fastTrackBasisFunctions") != null
              && parameters.getString("fastTrackBasisFunctions").length() > 0) {
            task.setFastTrackBasisFunctions(parameters.getString("fastTrackBasisFunctions"));
          }
          if (parameters.getString("fastTrackErrorMethod") != null
              && parameters.getString("fastTrackErrorMethod").length() > 0) {
            task.setFastTrackErrorMethod(parameters.getString("fastTrackErrorMethod"));
          }
          if (parameters.getString("fastTrackTrackingMethod") != null
              && parameters.getString("fastTrackTrackingMethod").length() > 0) {
            task.setFastTrackTrackingMethod(parameters.getString("fastTrackTrackingMethod"));
          }
          if (parameters.getString("fastTrackBasisFunctions") != null
              && parameters.getString("fastTrackBasisFunctions").length() > 0) {
            task.setFastTrackBasisFunctions(parameters.getString("fastTrackBasisFunctions"));
          }
          task.setFastTrackEnableF1FrequencyHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableF1FrequencyHeuristic")));
          if (parameters.getString("fastTrackMaximumF1FrequencyValue") != null) {
            try {
              task.setFastTrackMaximumF1FrequencyValue(
                Integer.parseInt(parameters.getString("fastTrackMaximumF1FrequencyValue")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackMaximumF1FrequencyValue \""
                  +parameters.getString("fastTrackMaximumF1FrequencyValue")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.setFastTrackEnableF1BandwidthHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableF1BandwidthHeuristic")));
          if (parameters.getString("fastTrackMaximumF1BandwidthValue") != null) {
            try {
              task.setFastTrackMaximumF1BandwidthValue(
                Integer.parseInt(parameters.getString("fastTrackMaximumF1BandwidthValue")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackMaximumF1BandwidthValue \""
                  +parameters.getString("fastTrackMaximumF1BandwidthValue")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.setFastTrackEnableF2BandwidthHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableF2BandwidthHeuristic")));
          if (parameters.getString("fastTrackMaximumF2BandwidthValue") != null) {
            try {
              task.setFastTrackMaximumF2BandwidthValue(
                Integer.parseInt(parameters.getString("fastTrackMaximumF2BandwidthValue")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackMaximumF2BandwidthValue \""
                  +parameters.getString("fastTrackMaximumF2BandwidthValue")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.setFastTrackEnableF3BandwidthHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableF3BandwidthHeuristic")));
          if (parameters.getString("fastTrackMaximumF3BandwidthValue") != null) {
            try {
              task.setFastTrackMaximumF3BandwidthValue(
                Integer.parseInt(parameters.getString("fastTrackMaximumF3BandwidthValue")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackMaximumF3BandwidthValue \""
                  +parameters.getString("fastTrackMaximumF3BandwidthValue")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.setFastTrackEnableF4FrequencyHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableF4FrequencyHeuristic")));
          if (parameters.getString("fastTrackMinimumF4FrequencyValue") != null) {
            try {
              task.setFastTrackMinimumF4FrequencyValue(
                Integer.parseInt(parameters.getString("fastTrackMinimumF4FrequencyValue")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackMinimumF4FrequencyValue \""
                  +parameters.getString("fastTrackMinimumF4FrequencyValue")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.setFastTrackEnableRhoticHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableRhoticHeuristic")));
          task.setFastTrackEnableF3F4ProximityHeuristic(
            "true".equalsIgnoreCase(parameters.getString("fastTrackEnableF3F4ProximityHeuristic")));
          if (parameters.getString("fastTrackNumberOfSteps") != null) {
            try {
              task.setFastTrackNumberOfSteps(
                Integer.parseInt(parameters.getString("fastTrackNumberOfSteps")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackNumberOfSteps \""
                  +parameters.getString("fastTrackNumberOfSteps")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          if (parameters.getString("fastTrackNumberOfCoefficients") != null) {
            try {
              task.setFastTrackNumberOfCoefficients(
                Integer.parseInt(parameters.getString("fastTrackNumberOfCoefficients")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackNumberOfCoefficients \""
                  +parameters.getString("fastTrackNumberOfCoefficients")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          if (parameters.getString("fastTrackNumberOfFormants") != null) {
            try {
              task.setFastTrackNumberOfFormants(
                Integer.parseInt(parameters.getString("fastTrackNumberOfFormants")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "fastTrackNumberOfFormants \""
                  +parameters.getString("fastTrackNumberOfFormants")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.setFastTrackCoefficients(
            "true".equalsIgnoreCase(parameters.getString("fastTrackCoefficients")));

          task.setExtractMinimumPitch(
            "true".equalsIgnoreCase(parameters.getString("extractMinimumPitch")));
          task.setExtractMeanPitch(
            "true".equalsIgnoreCase(parameters.getString("extractMeanPitch")));
          task.setExtractMaximumPitch(
            "true".equalsIgnoreCase(parameters.getString("extractMaximumPitch")));
          
          if (parameters.getString("pitchDifferentiationLayerId") != null) {
            task.setPitchDifferentiationLayerId(
              parameters.getString("pitchDifferentiationLayerId"));
          }
          task.getPitchOtherPattern().clear();
          for (String value : parameters.getStrings("pitchOtherPattern")) {
            try {
              task.getPitchOtherPattern().add(Pattern.compile(value));
            } catch(PatternSyntaxException exception) {
              writeResponse(
                response, failureResult(
                  request, "pitchOtherPattern \""+value
                  +"\" is not a valid regular expression: "
                  + exception.getMessage())); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("pitchFloorDefault") != null) {
            try {
              task.setPitchFloorDefault(
                Integer.parseInt(parameters.getString("pitchFloorDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "pitchFloorDefault \""+parameters.getString("pitchFloorDefault")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.getPitchFloorOther().clear();
          for (String value : parameters.getStrings("pitchFloorOther")) {
            try {
              task.getPitchFloorOther().add(Integer.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "pitchFloorOther \""+value+"\" is not an integer.")); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("pitchCeilingDefault") != null) {
            try {
              task.setPitchCeilingDefault(
                Integer.parseInt(parameters.getString("pitchCeilingDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "pitchCeilingDefault \""+parameters.getString("pitchCeilingDefault")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.getPitchCeilingOther().clear();
          for (String value : parameters.getStrings("pitchCeilingOther")) {
            try {
              task.getPitchCeilingOther().add(Integer.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "pitchCeilingOther \""+value+"\" is not an integer.")); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("voicingThresholdDefault") != null) {
            try {
              task.setVoicingThresholdDefault(
                Double.parseDouble(parameters.getString("voicingThresholdDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request,
                  "voicingThresholdDefault \""+parameters.getString("voicingThresholdDefault")
                  +"\" is not a number.")); // TODO i18n
              return;
            }
          }
          task.getVoicingThresholdOther().clear();
          for (String value : parameters.getStrings("voicingThresholdOther")) {
            try {
              task.getVoicingThresholdOther().add(Double.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "voicingThresholdOther \""+value+"\" is not a number.")); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("scriptPitch") != null
              && parameters.getString("scriptPitch").length() > 0) {
            task.setScriptPitch(parameters.getString("scriptPitch"));
          }

          task.setExtractMaximumIntensity(
            "true".equalsIgnoreCase(parameters.getString("extractMaximumIntensity")));
          if (parameters.getString("intensityDifferentiationLayerId") != null) {
            task.setIntensityDifferentiationLayerId(
              parameters.getString("intensityDifferentiationLayerId"));
          }
          task.getIntensityOtherPattern().clear();
          for (String value : parameters.getStrings("intensityOtherPattern")) {
            try {
              task.getIntensityOtherPattern().add(Pattern.compile(value));
            } catch(PatternSyntaxException exception) {
              writeResponse(
                response, failureResult(
                  request, "intensityOtherPattern \""+value
                  +"\" is not a valid regular expression: "
                  + exception.getMessage())); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("intensityPitchFloorDefault") != null) {
            try {
              task.setIntensityPitchFloorDefault(
                Integer.parseInt(parameters.getString("intensityPitchFloorDefault")));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request,
                  "intensityPitchFloorDefault \""
                  +parameters.getString("intensityPitchFloorDefault")
                  +"\" is not an integer.")); // TODO i18n
              return;
            }
          }
          task.getIntensityPitchFloorOther().clear();
          for (String value : parameters.getStrings("intensityPitchFloorOther")) {
            try {
              task.getIntensityPitchFloorOther().add(Integer.valueOf(value));
            } catch(NumberFormatException exception) {
              writeResponse(
                response, failureResult(
                  request, "intensityPitchFloorOther \""
                  +value+"\" is not an integer.")); // TODO i18n
              return;
            }
          } // next value
          if (parameters.getString("scriptIntensity") != null
              && parameters.getString("scriptIntensity").length() > 0) {
            task.setScriptIntensity(parameters.getString("scriptIntensity"));
          }
          
          task.setExtractCOG1("true".equalsIgnoreCase(parameters.getString("extractCOG1")));
          task.setExtractCOG2("true".equalsIgnoreCase(parameters.getString("extractCOG2")));
          task.setExtractCOG23("true".equalsIgnoreCase(parameters.getString("extractCOG23")));
          
          if (parameters.getString("script") != null
              && parameters.getString("script").length() > 0) {
            task.setCustomScript(parameters.getString("script"));
          }
          
          for (String value : parameters.getStrings("attributes")) {
            task.getAttributes().add(value);
          }

          // ensure number of patterns match values
          if (task.getFormantOtherPattern().size() != task.getFormantCeilingOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "formantOtherPattern and formantCeilingOther must have the same number of values.")); // TODO i18n
            return;
          }
          if (task.getPitchOtherPattern().size() != task.getPitchFloorOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "pitchOtherPattern and pitchFloorOther must have the same number of values.")); // TODO i18n
            return;
          }
          if (task.getPitchOtherPattern().size() != task.getPitchCeilingOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "pitchOtherPattern and pitchCeilingOther must have the same number of values.")); // TODO i18n
            return;
          }
          if (task.getPitchOtherPattern().size() != task.getVoicingThresholdOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "pitchOtherPattern and voicingThresholdOther must have the same number of values.")); // TODO i18n
            return;
          }
          if (task.getIntensityOtherPattern().size() != task.getIntensityPitchFloorOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "intensityOtherPattern and intensityPitchFloorOther must have the same number of values.")); // TODO i18n
            return;
          }
          if (task.getFastTrackOtherPattern().size() != task.getFastTrackLowestAnalysisFrequencyOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "fastTrackOtherPattern and fastTrackLowestAnalysisFrequencyOther must have the same number of values.")); // TODO i18n
            return;
          }
          if (task.getFastTrackOtherPattern().size() != task.getFastTrackHighestAnalysisFrequencyOther().size()) {
            writeResponse(
              response, failureResult(
                request,
                "fastTrackOtherPattern and fastTrackHighestAnalysisFrequencyOther must have the same number of values.")); // TODO i18n
            return;
          }

          // start the task
          task.setName(uploadedCsvFile.getName());
          if (request.getRemoteUser() != null) {	
            task.setWho(request.getRemoteUser());
          } else {
            task.setWho(request.getRemoteHost());
          }
          task.start();
          
          // return its ID
          JsonObjectBuilder jsonResult = Json.createObjectBuilder()
            .add("threadId", task.getId());
          writeResponse(
            response, successResult(request, jsonResult.build(), null));
          
        } finally {
          // TODO cacheStore(store);
        }
      } catch(Exception ex) {
        throw new ServletException(ex);
      }
  }
  
   private static final long serialVersionUID = -1;
} // end of class SerializeGraphs
