//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.labbcat.server.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.*;
import nzilbb.util.IO;

/**
 * Task that upgrades LaBB-CAT by selectively unpacking a given .war file.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Upgrader extends Task {

  protected static Upgrader upgrader = null;  
  /**
   * Provides the currently-running upgrader task, if any.
   * @return The currently-running upgrader task, if any.
   */
  public static Upgrader getUpgrader() {
    return upgrader;
  } // end of getUpgrader()
  
  /**
   * The .war package to upgrade from.
   * @see #getWar()
   * @see #setWar(File)
   */
  protected File war;
  /**
   * Getter for {@link #war}: The .war package to upgrade from.
   * @return The .war package to upgrade from.
   */
  public File getWar() { return war; }
  /**
   * Setter for {@link #war}: The .war package to upgrade from.
   * @param newWar The .war package to upgrade from.
   */
  public Upgrader setWar(File newWar) { war = newWar; return this; }
  
  /**
   * LaBB-CAT root directory, to where the contents of the uploader should be unpacked.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File dir;
  /**
   * Getter for {@link #dir}: LaBB-CAT root directory, to where the
   * contents of the uploader should be unpacked. 
   * @return LaBB-CAT root directory, to where the contents of the
   * uploader should be unpacked. 
   */
  public File getDir() { return dir; }
  /**
   * Setter for {@link #dir}: LaBB-CAT root directory, to where the
   * contents of the uploader should be unpacked. 
   * @param newDir LaBB-CAT root directory, to where the contents of
   * the uploader should be unpacked. 
   */
  public Upgrader setDir(File newDir) { dir = newDir; return this; }
  
  /**
   * Messages logged by the upgrader.
   * @see #getMessages()
   * @see #setMessages(Vector<String>)
   */
  protected Vector<String> messages = new Vector<String>();
  /**
   * Getter for {@link #messages}: Messages logged by the upgrader.
   * @return Messages logged by the upgrader.
   */
  public Vector<String> getMessages() { return messages; }
  
  /**
   * Subclass constructor.
   */
  public Upgrader() {
  }
  
  /**
   * Constructor.
   * @param war The .war package file to upgrade from.
   * @param dir The directory into which files should be unpacked.
   */
  public Upgrader(File war, File dir) {
    this.war = war;
    this.dir = dir;
  }

  /**
   * Sets the thread status.
   * @param sMessage a status message to display to anyone who's watching the thread.
   */
  @Override public void setStatus(String message) {
    System.err.println(message);
    messages.add(message);
    super.setStatus(message);
  }
  
  /**
   * Runs the upgrade.
   */
  public void run() {
    runStart();
    upgrader = this;
    try {

      setStatus("Upgrading from package " + war.getName() + " ...");
      
      File oldVersionFile = new File(dir, "version.txt");
      String oldVersion = IO.InputStreamToString(new FileInputStream(oldVersionFile));
      
      // TODO handle migration
      
      // unpack contents
      String lastFile = null;
      JarFile jar = new JarFile(war);
      String newVersion = IO.InputStreamToString(
        jar.getInputStream(jar.getJarEntry("version.txt")));
      setStatus(oldVersion + " → " + newVersion);
      
      try {
        Enumeration<JarEntry> enEntries = jar.entries();
        while (enEntries.hasMoreElements()) {
          JarEntry entry = enEntries.nextElement();
          if (!entry.isDirectory()
              && !entry.getName().equals("UnJar.class")
              && !entry.getName().endsWith("web.xml") // just in case
              && !entry.getName().startsWith("META-INF") // just in case
              && !entry.getName().equals("install.jsp") // just in case
              && !entry.getName().equals("local.css") // don't overwrite
              && !entry.getName().equals("custom.css") // don't overwrite
              && !entry.getName().equals("favicon.ico") // don't overwrite
              && !entry.getName().equals("favicon.svg") // don't overwrite
              && !entry.getName().endsWith("migrate.sql") // processed above
            ) {
            File parent = dir;
            String sFileName = entry.getName();
            setStatus("Extracting " + entry.getName());
            lastFile = entry.getName();
            StringTokenizer stPathParts = new StringTokenizer(entry.getName(), "/");
            if (stPathParts.countTokens() > 1) { // complex path
              // ensure that the required directories exist
              sFileName = stPathParts.nextToken();
              
              while(stPathParts.hasMoreTokens()) {
                // previous token was not the last, so it
                // must be a directory
                parent = new File(parent, sFileName);
                if (!parent.exists()) {
                  parent.mkdir();
                }
		
                sFileName = stPathParts.nextToken();
              } // next token
            }
            File file = new File(parent, sFileName);
            
            // get input stream
            InputStream in = jar.getInputStream(entry);

            // unpack file
            IO.SaveInputStreamToFile(in, file);
            
          } else {
            setStatus("Skipping " + entry.getName());
          }
        } // next entry
        war.delete();
        // setStatus("Deleted " + war.getPath());
        
        // trigger restart
        File webXmlFile = new File(new File(dir, "WEB-INF"), "web.xml");
        setStatus("Triggering restart...");
        webXmlFile.setLastModified(new java.util.Date().getTime());
        
      } catch(Throwable exception) {
        setStatus("Error extracting file \"" + lastFile + "\": " + exception);
        setLastException(exception);
      }      
      
    } catch(FileNotFoundException exception) {
      setStatus("Error getting current version: " + exception);
      setLastException(exception);
    } catch(IOException exception) {
      setStatus("Error processing jar: " + exception);
      setLastException(exception);
    } finally {
      runEnd();
      bRunning = true; // leaving it 'running' so that the UI waits for restart
      waitToDie();
      upgrader = null;
    }
  } // end of run()
  
  /**
   * Release resources.
   */
  @Override public void release() {
    if (war != null) war.delete();
    super.release();
  }
  
}
