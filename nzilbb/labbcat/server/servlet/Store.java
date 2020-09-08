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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Vector;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import nzilbb.ag.*;
import nzilbb.labbcat.server.db.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Controller that handles
 * <a href="https://nzilbb.github.io/ag/javadoc/nzilbb/ag/IGraphStore.html">nzilbb.ag.IGraphStore</a>
 * requests. This includes all requests supported by {@link StoreQuery}.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet({"/edit/store/*", "/api/edit/store/*"})
public class Store extends StoreQuery {
   // Attributes:

   // Methods:
   
   /**
    * Default constructor.
    */
   public Store() {
   } // end of constructor

   /** 
    * Initialise the servlet
    */
   public void init() {
      super.init();
   }

   // StoreQuery overrides

   /**
    * Interprets the URL path, and executes the corresponding function on the store. This
    * method is an override of 
    * {@link StoreQuery#invokeFunction(HttpServletRequest,HttpServletResponse,SqlGraphStoreAdministration)}.
    * <p> This implementation only allows POST HTTP requests.
    * @param request The request.
    * @param response The response.
    * @param store The connected graph store.
    * @return The response to send to the caller, or null if the request could not be interpreted.
    */
   @Override
   protected JsonObject invokeFunction(HttpServletRequest request, HttpServletResponse response, SqlGraphStoreAdministration store)
      throws ServletException, IOException, StoreException, PermissionException, GraphNotFoundException {
      try { // check they have edit permission
         if (!isUserInRole("edit", request, store.getConnection())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return failureResult(request, "User has no edit permission.");
         }
      } catch(SQLException x) {
         response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         return failureResult(x);
      }
      
      JsonObject json = null;
      String pathInfo = request.getPathInfo().toLowerCase(); // case-insensitive
      // only allow POST requests
      if (request.getMethod().equals("POST")) {
         
         if (pathInfo.endsWith("createannotation")) {
            json = createAnnotation(request, response, store);
         } else if (pathInfo.endsWith("destroyannotation")) {
            json = destroyAnnotation(request, response, store);
         } else if (pathInfo.endsWith("deletetranscript")
                    // support deprecated name
                    || pathInfo.endsWith("deletegraph")) {
            json = deleteTranscript(request, response, store);
         }
      } // only if it's a POST request
      
      if (json == null) { // either not POST or not a recognized function
         json = super.invokeFunction(request, response, store);
      }
      return json;
   } // end of invokeFunction()

   // IGraphStore method handlers

   // TODO saveTranscript
   
   /**
    * Implementation of {@link nzilbb.ag.IGraphStoreQuery#createAnnotation(String,String,String,String,String,Integer,String)}
    * @param request The HTTP request.
    * @param request The HTTP response.
    * @param store A graph store object.
    * @return A JSON response for returning to the caller.
    */
   protected JsonObject createAnnotation(
      HttpServletRequest request, HttpServletResponse response, SqlGraphStoreAdministration store)
      throws ServletException, IOException, StoreException, PermissionException, GraphNotFoundException {
      Vector<String> errors = new Vector<String>();
      String id = request.getParameter("id");
      if (id == null) errors.add(localize(request, "No ID specified."));
      String fromId = request.getParameter("fromId");
      if (fromId == null) errors.add(localize(request, "No From ID specified."));
      String toId = request.getParameter("toId");
      if (toId == null) errors.add(localize(request, "No To ID specified."));
      String layerId = request.getParameter("layerId");
      if (layerId == null) errors.add(localize(request, "No layer ID specified."));
      String label = request.getParameter("label");
      if (label == null) errors.add(localize(request, "No label specified."));
      Integer confidence = null;
      if (request.getParameter("confidence") == null) {
         errors.add(localize(request, "No confidence specified."));
      } else {
         try {
            confidence = Integer.valueOf(request.getParameter("confidence"));
         } catch(NumberFormatException x) {
            errors.add(localize(request, "Invalid confidence: {0}", x.getMessage()));
         }
      }
      String parentId = request.getParameter("parentId");
      if (parentId == null) errors.add(localize(request, "No parent ID specified."));
      if (errors.size() > 0) return failureResult(errors);
      return successResult(
         request, store.createAnnotation(id, fromId, toId, layerId, label, confidence, parentId), null);
   }      
   
   /**
    * Implementation of {@link nzilbb.ag.IGraphStoreQuery#destroyAnnotation(String,String)}
    * @param request The HTTP request.
    * @param request The HTTP response.
    * @param store A graph store object.
    * @return A JSON response for returning to the caller.
    */
   protected JsonObject destroyAnnotation(
      HttpServletRequest request, HttpServletResponse response, SqlGraphStoreAdministration store)
      throws ServletException, IOException, StoreException, PermissionException, GraphNotFoundException {
      Vector<String> errors = new Vector<String>();
      String id = request.getParameter("id");
      if (id == null) errors.add(localize(request, "No ID specified."));
      String annotationId = request.getParameter("annotationId");
      if (annotationId == null) errors.add(localize(request, "No annotation ID specified."));
      if (errors.size() > 0) return failureResult(errors);
      store.destroyAnnotation(id, annotationId);
      return successResult(request, null, "Annotation deleted: {0}", id);
   }      
   // TODO saveParticipant
   // TODO saveMedia
   // TODO saveSource
   // TODO saveEpisodeDocument

   /**
    * Implementation of {@link nzilbb.ag.IGraphStoreQuery#deleteGraph(String)}
    * @param request The HTTP request.
    * @param request The HTTP response.
    * @param store A graph store object.
    * @return A JSON response for returning to the caller.
    */
   protected JsonObject deleteTranscript(
      HttpServletRequest request, HttpServletResponse response, SqlGraphStoreAdministration store)
      throws ServletException, IOException, StoreException, PermissionException, GraphNotFoundException {
      Vector<String> errors = new Vector<String>();
      String id = request.getParameter("id");
      if (id == null) errors.add(localize(request, "No id specified."));
      if (errors.size() > 0) return failureResult(errors);
      store.deleteTranscript(id);
      return successResult(request, null, "Transcript deleted: {0}", id);
   }      
   
   private static final long serialVersionUID = 1;
} // end of class Store
