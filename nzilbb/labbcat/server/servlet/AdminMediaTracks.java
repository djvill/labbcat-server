//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Servlet that allows administration of rows in the the <em> corpus </em> table.
 * <p> See <a href="package-summary.html#/api/admin/mediaTracks">API summary</a> for more details.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet(urlPatterns = "/api/admin/mediatracks/*", loadOnStartup = 20)
@RequiredRole("admin")
public class AdminMediaTracks extends TableServletBase {   
   
   public AdminMediaTracks() {
      super("media_track", // table
            new Vector<String>() {{ // primary keys
               add("suffix");
            }},
            new Vector<String>() {{ // columns
               add("description");
               add("display_order");
            }},
            null, // where
            "display_order", // order
            true, // create
            true, // read
            true, // update
            true); // delete
      emptyKeyAllowed = true;
   }

   /**
    * Validates a record before UPDATEing it.
    * @param record The incoming record to validate.
    * @param connection A connection to th database.
    * @return A list of validation errors, which should be null if the record is valid.
    */
   protected List<String> validateBeforeUpdate(JSONObject record, Connection connection) {
      if (!record.has("display_order") || record.isNull("display_order")
          || record.get("display_order").equals("")) {
         try {
            // default is one more that the MAX
            PreparedStatement sql = connection.prepareStatement(
               "SELECT COALESCE(MAX(display_order) + 1, 1) FROM media_track");
            ResultSet rs = sql.executeQuery();
            try {
               rs.next();
               record.put("display_order", rs.getInt(1));
            } finally {
               rs.close();
               sql.close();
            }            
         } catch(SQLException exception) {
            log("ERROR getting default value for display_order: " + exception);
            record.put("display_order", 0);
         }
      } 
      return null;
   } // end of validateBeforeUpdate()
   
   private static final long serialVersionUID = 1;
} // end of class AdminMediaTracks
