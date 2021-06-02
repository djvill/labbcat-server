//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <tt>/api/admin/systemattributes</tt> : Administration of <em> system attribute </em> records.
 *  <p> Allows administration (Read/Update) of system attribute records via
 *  JSON-encoded objects with the following attributes:
 *   <dl>
 *    <dt> attribute </dt><dd> ID of the attribute. </dd>
 *    <dt> type </dt><dd> The type of the attribute - "string", "integer", "boolean",
 *                         "select", etc.  </dd>
 *    <dt> style </dt><dd> Style definition which depends on <q> type </dt><dd> - e.g. whether
 *                          the "boolean" is shown as a checkbox or radio buttons, etc.  </dd>
 *    <dt> label </dt><dd> User-facing label for the attribute. </dd>
 *    <dt> description </dt><dd> User-facing (long) description of the attribute. </dd>
 *    <dt> options </dt><dd> If <q> type </dt><dd> is "select", this is an object defining the
 *                            valid options for the attribute, where the object key is the
 *                            attribute value and the key's value is the user-facing label
 *                            for the option.  </dd> 
 *    <dt> value </dt><dd> The value of the attribute. </dd>
 *   </dl>
 *  <p> The following operations, specified by the HTTP method, are supported:
 *   <dl>
 *    <dt> GET </dt><dd> Read the records. 
 *     <ul>
 *      <li><em> Response Body </em> - the standard JSON envelope, with the model as a
 *       corresponding list of records.  </li>
 *      <li><em> Response Status </em>
 *        <ul>
 *         <li><em> 200 </em> : The records could be listed. </li>
 *        </ul>
 *      </li>
 *     </ul></dd> 
 *    
 *    <dt> PUT </dt><dd> Update an existing record, specified by the <var> systemAttribute </var> given in the
 *    request body.
 *     <ul>
 *      <li><em> Request Body </em> - a JSON-encoded object representing the record. </li>
 *      <li><em> Response Body </em> - the standard JSON envelope, with the model as an
 *       object representing the record. </li> 
 *      <li><em> Response Status </em>
 *        <ul>
 *         <li><em> 200 </em> : The record was successfully updated. </li>
 *         <li><em> 400 </em> : The record has type == "readonly" found. </li>
 *         <li><em> 404 </em> : The record was not found. </li>
 *        </ul>
 *      </li>
 *     </ul></dd> 
 *   </dl>
 *  </p>
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet({"/api/admin/systemattributes"} )
@RequiredRole("admin")
public class AdminSystemAttributes extends LabbcatServlet {

  /**
   * Default constructor.
   */
  public AdminSystemAttributes() {
  } // end of constructor
   
  /**
   * GET handler lists all rows. 
   * <p> The return is JSON encoded, unless the "Accept" request header, or the "Accept"
   * request parameter, is "text/csv", in which case CSV is returned.
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
      Connection connection = newConnection();
      try {
        if (!hasAccess(request, response, connection)) {
          return;
        } else {
          response.setContentType("application/json");               
          response.setCharacterEncoding("UTF-8");
          try {
            PreparedStatement sql = connection.prepareStatement(
              "SELECT attribute, type, style, label, description, value"
              +" FROM attribute_definition"
              +" LEFT OUTER JOIN system_attribute"
              +" ON attribute_definition.attribute = system_attribute.name"
              +" WHERE class_id = ''"
              +" ORDER BY display_order, attribute");
            PreparedStatement sqlOptions = connection.prepareStatement(
              "SELECT value, description"
              +" FROM attribute_option"
              +" WHERE class_id = '' AND attribute = ?"
              +" ORDER BY value");
            ResultSet rs = sql.executeQuery();
            JsonGenerator jsonOut = Json.createGenerator(response.getWriter());
            startResult(jsonOut, true);
            try {
              while (rs.next()) {
                jsonOut.writeStartObject();
                try {
                  jsonOut.write("attribute", rs.getString("attribute"));
                  String type = rs.getString("type");
                  if (type.startsWith("SELECT ")) type = "select";
                  jsonOut.write("type", type);
                  jsonOut.write("style", rs.getString("style"));
                  jsonOut.write("label", rs.getString("label"));
                  jsonOut.write("description", rs.getString("description"));
                  // are there options?
                  PreparedStatement sqlOptionQuery = null;
                           
                  if ("select".equals(rs.getString("type"))) { // predefined options
                    sqlOptionQuery = sqlOptions;
                    sqlOptionQuery.setString(1, rs.getString("attribute"));
                  } else if (rs.getString("type").startsWith("SELECT ")) { // SQL based
                    sqlOptionQuery = connection.prepareStatement(rs.getString("type"));
                  }
                  if (sqlOptionQuery != null) {
                    ResultSet rsOptions = sqlOptionQuery.executeQuery();
                    jsonOut.writeStartObject("options");
                    try {
                      while (rsOptions.next()) {
                        jsonOut.write(rsOptions.getString(1), rsOptions.getString(2));
                      } // next option
                    } finally {
                      rsOptions.close();
                      if (sqlOptionQuery != sqlOptions) {
                        sqlOptionQuery.close();
                      }
                      jsonOut.writeEnd(); // Object
                    }                              
                  } // options
                  if (rs.getString("value") != null) {
                    jsonOut.write("value", rs.getString("value"));
                  } else {
                    jsonOut.write("value", "");
                  }
                } finally {
                  jsonOut.writeEnd(); // Object
                }
              } // next attribute
            } finally {
              rs.close();
              sql.close();
              sqlOptions.close();
            }
            endSuccessResult(request, jsonOut, null);
          } catch(SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log("AdminSystemAttributes GET: ERROR: " + exception);
            response.setContentType("application/json");
            writeResponse(response, failureResult(exception));
          }
        }
      } finally {
        connection.close();
      }
    } catch(SQLException exception) {
      log("AdminSystemAttributes GET: Couldn't connect to database: " + exception);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      writeResponse(response, failureResult(exception));
    }      
  }

  /**
   * PUT handler - update an existing row.
   */
  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
      Connection connection = newConnection();
      try {
        if (!hasAccess(request, response, connection)) {
          return;
        } else {
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
               
          JsonReader reader = Json.createReader( // ensure we read as UTF-8
            new InputStreamReader(request.getInputStream(), "UTF-8"));
          // incoming object:
          JsonObject json = reader.readObject();

          // check it exists and isn't readonly
          PreparedStatement sqlCheck = connection.prepareStatement(
            "SELECT type FROM attribute_definition"
            +" WHERE attribute = ? AND class_id = ''");
          sqlCheck.setString(1, json.getString("attribute"));
          ResultSet rsCheck = sqlCheck.executeQuery();
          try {
            if (rsCheck.next()) { // readonly
              if (!"readonly".equals(rsCheck.getString("type"))) { // not readonly
                PreparedStatement sql = connection.prepareStatement(
                  "UPDATE system_attribute SET value = ? WHERE name = ?");
                sql.setString(1, json.getString("value"));
                sql.setString(2, json.getString("attribute"));
                int rows = sql.executeUpdate();
                if (rows == 0) { // no value row yet
                  // insert one
                  sql.close();
                  sql = connection.prepareStatement(
                    "INSERT INTO system_attribute (value, name) VALUES (?, ?)");
                  sql.setString(1, json.getString("value"));
                  sql.setString(2, json.getString("attribute"));
                  rows = sql.executeUpdate();
                  if (rows == 0) { // shouldn't be possible
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    writeResponse(
                      response, failureResult(
                        request, "Record not updated: {0}", json.getString("attribute")));
                  }
                } else {
                  writeResponse(
                    response, successResult(request, json, "Record updated."));
                }
                sql.close();
              } else { // readonly
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeResponse(
                  response, failureResult(
                    request, "Read-only record: {0}", json.getString("attribute")));
              }
            } else { // not found
              response.setStatus(HttpServletResponse.SC_NOT_FOUND);
              writeResponse(
                response, failureResult(
                  request, "Record not found: {0}", json.getString("attribute")));
            }
          } finally {
            rsCheck.close();
            sqlCheck.close();
          }
        }
      } finally {
        connection.close();
      }
    } catch(SQLException exception) {
      log("TableServletBase PUT: Couldn't connect to database: " + exception);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      writeResponse(response, failureResult(exception));
    }      
  }
   
  private static final long serialVersionUID = 1;
}
