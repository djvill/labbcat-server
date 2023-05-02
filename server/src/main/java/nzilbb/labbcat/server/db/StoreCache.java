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
package nzilbb.labbcat.server.db;

import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Cache for Graph Stores, which supplies a {@link GraphStoreAdministration} object for
 * use, and then accepts it back again for reuse later.
 * <p> This ensures that resources are shared or closed as appropriate.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface StoreCache
  extends Supplier<SqlGraphStore>, Consumer<SqlGraphStore> {
} 
