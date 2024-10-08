//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//

package nzilbb.labbcat.server.search;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.junit.*;
import static org.junit.Assert.*;

public class TestMatrix {

  /** Ensure that serialization to JSON works. */
  @Test public void toJson() {
    Matrix m = new Matrix()
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch()
                                  .setId("orthography")
                                  .setPattern("the"))
                 .setAdj(3))
      .addColumn(new Column()
                 .addLayerMatch(new LayerMatch()
                                  .setId("phonology")
                                  .setNot(true)
                                  .setPattern("[aeiou].*")
                                  .setAnchorStart(true)
                                  .setMinString("") // should end up null
                                  .setMaxString("")  // should end up null
                                  .ensurePatternAnchored()) // test effects of anchoring 
                 .addLayerMatch(new LayerMatch()
                                  .setId("syllableCount")
                                  .setMin(2.0)
                                  .setMax(3.0)
                                  .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch()
                                  .setId("orthography")
                                  .setPattern("")
                                  .setTarget(true)));
    assertEquals(
      "JSON serialization - toJson",
      "{\"columns\":["
      +"{\"adj\":3,"
      +"\"layers\":{"
      +"\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"the\"}]}}"
      +",{\"adj\":1,"
      +"\"layers\":{"
      +"\"phonology\":[{\"anchorStart\":true,\"id\":\"phonology\",\"not\":true,\"pattern\":\"^([aeiou].*)$\"}],"
      +"\"syllableCount\":[{\"anchorEnd\":true,\"id\":\"syllableCount\",\"max\":3.0,\"min\":2.0}],"
      +"\"orthography\":[{\"id\":\"orthography\",\"target\":true}]}}]}",
      m.toJson().toString());
    
    assertEquals(
      "JSON serialization - toString",
      "{\"columns\":["
      +"{\"adj\":3,"
      +"\"layers\":{"
      +"\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"the\"}]}}"
      +",{\"adj\":1,"
      +"\"layers\":{"
      +"\"phonology\":[{\"anchorStart\":true,\"id\":\"phonology\",\"not\":true,\"pattern\":\"^([aeiou].*)$\"}],"
      +"\"syllableCount\":[{\"anchorEnd\":true,\"id\":\"syllableCount\",\"max\":3.0,\"min\":2.0}],"
      +"\"orthography\":[{\"id\":\"orthography\",\"target\":true}]}}]}",
      m.toString());
    
    assertEquals(
      "Description",
      "orthography=the phonology-NOT-[aeiou].* syllableCount>=2<3",
      m.getDescription());
  }

  /** Ensure that deserialization from JSON works. */
  @Test public void fromJson() {
    JsonObject json = Json.createObjectBuilder() // TODO use nzilbb.labbcat.PatternBuilder
      .add("columns", Json.createArrayBuilder()
           .add(Json.createObjectBuilder()
                .add("layers", Json.createObjectBuilder()
                     .add("orthography", Json.createObjectBuilder()
                          .add("id", "orthography")
                          .add("pattern", "the")))
                .add("adj", "3")) // encoding as string must work too
           .add(Json.createObjectBuilder()
                .add("layers", Json.createObjectBuilder()
                     .add("phonology", Json.createObjectBuilder()
                          .add("id", "phonology")
                          .add("min", "") // should end up null
                          .add("max", "") // should end up null
                          .add("not", true)
                          .add("pattern", "[aeiou].*")
                          .add("anchorStart", true))
                     .add("syllableCount", Json.createObjectBuilder()
                          .add("id", "syllableCount")
                          .add("min", "2") // expressed as a string
                          .add("max", 3.0) // or as a number
                          .add("anchorEnd", true))
                     .add("orthography", Json.createObjectBuilder()
                          .add("id", "orthography")
                          .add("pattern", "")
                          .add("target", true)))))      
      .build();

    Matrix m = (Matrix)(new Matrix().fromJson(json));
    assertEquals("Number of columns", 2, m.getColumns().size());

    Column col = m.getColumns().get(0);
    assertEquals("First col: adj", 3, col.getAdj());
    assertEquals("First col: number of layers", 1, col.getLayers().size());

    LayerMatch layerMatch = col.getFirstLayerMatch("orthography");
    assertNotNull("First col: layer is orthography", layerMatch);
    assertEquals("First col: layer id", "orthography", layerMatch.getId());
    assertNull("First col: layer negation null", layerMatch.getNot());
    layerMatch.setNullBooleans();
    assertFalse("First col: setNullBooleans - layer negation false", layerMatch.getNot());
    assertEquals("First col: layer pattern", "the", layerMatch.getPattern());
    assertNull("First col: layer min", layerMatch.getMin());
    assertNull("First col: layer max", layerMatch.getMax());
    assertFalse("First col: layer target", layerMatch.getTarget());
    assertFalse("First col: layer anchorStart", layerMatch.getAnchorStart());
    assertFalse("First col: layer anchorEnd", layerMatch.getAnchorEnd());
    
    col = m.getColumns().get(1);
    assertEquals("Second col: adj", 1, col.getAdj());
    assertEquals("Second col: number of layers", 3, col.getLayers().size());

    layerMatch = col.getFirstLayerMatch("phonology");
    assertNotNull("Second col: phonology layer exists", layerMatch);
    assertEquals("Second col: phonology layer id", "phonology", layerMatch.getId());
    assertTrue("Second col: phonology layer negation", layerMatch.getNot());
    assertEquals("Second col: phonology layer pattern", "[aeiou].*", layerMatch.getPattern());
    assertNull("Second col: phonology layer min", layerMatch.getMin());
    assertNull("Second col: phonology layer max", layerMatch.getMax());
    assertNull("Second col: phonology layer target", layerMatch.getTarget());
    assertTrue("Second col: phonology layer anchorStart", layerMatch.getAnchorStart());
    assertNull("Second col: phonology layer anchorEnd", layerMatch.getAnchorEnd());

    layerMatch = col.getFirstLayerMatch("syllableCount");
    assertNotNull("Second col: syllableCount layer exists", layerMatch);
    assertEquals("Second col: syllableCount layer id", "syllableCount", layerMatch.getId());
    assertNull("Second col: syllableCount layer pattern", layerMatch.getPattern());
    assertNull("Second col: syllableCount negation null", layerMatch.getNot());
    assertEquals("Second col: syllableCount layer min", Double.valueOf(2), layerMatch.getMin());
    assertEquals("Second col: syllableCount layer max", Double.valueOf(3), layerMatch.getMax());
    assertNull("Second col: syllableCount layer target", layerMatch.getTarget());
    assertNull("Second col: syllableCount layer anchorStart", layerMatch.getAnchorStart());
    assertTrue("Second col: syllableCount layer anchorEnd", layerMatch.getAnchorEnd());

    layerMatch = col.getFirstLayerMatch("orthography");
    assertNotNull("Second col: orthography layer exists", layerMatch);
    assertEquals("Second col: orthography layer id", "orthography", layerMatch.getId());
    assertNull("Second col: orthography layer negation null", layerMatch.getNot());
    assertNull("Second col: orthography layer pattern", layerMatch.getPattern());
    assertNull("Second col: orthography layer min", layerMatch.getMin());
    assertNull("Second col: orthography layer max", layerMatch.getMax());
    assertTrue("Second col: orthography layer target", layerMatch.getTarget());
    assertNull("Second col: orthography layer anchorStart", layerMatch.getAnchorStart());
    assertNull("Second col: orthography layer anchorEnd", layerMatch.getAnchorEnd());
  }
  
  /** Ensure that word-internal multi segment (de)serialization works. */
  @Test public void segmentContextQuery() {
    JsonObject json = Json.createObjectBuilder()
      .add("columns", Json.createArrayBuilder()
           .add(Json.createObjectBuilder()
                .add("layers", Json.createObjectBuilder()
                     .add("segment", Json.createArrayBuilder()
                          .add(Json.createObjectBuilder() // starts with p/t/k
                               .add("id", "doesn't matter")
                               .add("pattern", "[ptk]")
                               .add("anchorStart", true))
                          .add(Json.createObjectBuilder() // followed by vowel
                               .add("id", "this is ignored")
                               .add("pattern", "[aeiou]")
                               .add("target", true))
                       )
                  )
             )
        )
      .build();
    Matrix m = (Matrix)(new Matrix().fromJson(json));
    assertEquals("Number of columns", 1, m.getColumns().size());

    Column col = m.getColumns().get(0);
    assertEquals("col: adj", 1, col.getAdj());
    assertEquals("col: number of layers", 1, col.getLayers().size());
    assertNotNull("layer is segment", col.getLayers().get("segment"));
    assertEquals("layer: number of matches", 2, col.getLayers().get("segment").size());

    LayerMatch layerMatch = col.getLayers().get("segment").get(0);
    assertEquals("First match: layer id", "segment", layerMatch.getId());
    assertNull("First match: layer negation null", layerMatch.getNot());
    layerMatch.setNullBooleans();
    assertFalse("First match: setNullBooleans - layer negation false", layerMatch.getNot());
    assertEquals("First match: layer pattern", "[ptk]", layerMatch.getPattern());
    assertNull("First match: layer min", layerMatch.getMin());
    assertNull("First match: layer max", layerMatch.getMax());
    assertFalse("First match: layer target", layerMatch.getTarget());
    assertTrue("First match: layer anchorStart", layerMatch.getAnchorStart());
    assertFalse("First match: layer anchorEnd", layerMatch.getAnchorEnd());
    
    layerMatch = col.getLayers().get("segment").get(1);
    assertEquals("Second match: layer id", "segment", layerMatch.getId());
    layerMatch.setNullBooleans();
    assertFalse("Second match: setNullBooleans - layer negation false", layerMatch.getNot());
    assertEquals("Second match: layer pattern", "[aeiou]", layerMatch.getPattern());
    assertNull("Second match: layer min", layerMatch.getMin());
    assertNull("Second match: layer max", layerMatch.getMax());
    assertTrue("Second match: layer target", layerMatch.getTarget());
    assertFalse("Second match: layer anchorStart", layerMatch.getAnchorStart());
    assertFalse("Second match: layer anchorEnd", layerMatch.getAnchorEnd());
  }
  
  /** Ensure that deserialization from JSON-encoded String works. */
  @Test public void fromJsonString() {
    JsonObject json = Json.createObjectBuilder()
      .add("columns", Json.createArrayBuilder()
           .add(Json.createObjectBuilder()
                .add("layers", Json.createObjectBuilder()
                     .add("orthography", Json.createObjectBuilder()
                          .add("id", "orthography")
                          .add("pattern", "the")))
                .add("adj", 3))
           .add(Json.createObjectBuilder()
                .add("layers", Json.createObjectBuilder()
                     .add("phonology", Json.createObjectBuilder()
                          .add("id", "phonology")
                          .add("pattern", "[aeiou].*")
                          .add("anchorStart", true))
                     .add("syllableCount", Json.createObjectBuilder()
                          .add("id", "syllableCount")
                          .add("min", 2.0) // number
                          .add("max", "3") // or string
                          .add("anchorEnd", true))
                     .add("orthography", Json.createObjectBuilder()
                          .add("id", "orthography")
                          .add("target", true)))))      
      .build();

    Matrix m = new Matrix().fromJsonString(json.toString());
    assertEquals("Number of columns", 2, m.getColumns().size());

    Column col = m.getColumns().get(0);
    assertEquals("First col: adj", 3, col.getAdj());
    assertEquals("First col: number of layers", 1, col.getLayers().size());

    LayerMatch layerMatch = col.getFirstLayerMatch("orthography");
    assertNotNull("First col: layer is orthography", layerMatch);
    assertEquals("First col: layer id", "orthography", layerMatch.getId());
    assertEquals("First col: layer pattern", "the", layerMatch.getPattern());
    assertNull("First col: layer min", layerMatch.getMin());
    assertNull("First col: layer max", layerMatch.getMax());
    assertNull("First col: layer target", layerMatch.getTarget());
    assertNull("First col: layer anchorStart", layerMatch.getAnchorStart());
    assertNull("First col: layer anchorEnd", layerMatch.getAnchorEnd());
    
    col = m.getColumns().get(1);
    assertEquals("Second col: adj", 1, col.getAdj());
    assertEquals("Second col: number of layers", 3, col.getLayers().size());

    layerMatch = col.getFirstLayerMatch("phonology");
    assertNotNull("Second col: phonology layer exists", layerMatch);
    assertEquals("Second col: phonology layer id", "phonology", layerMatch.getId());
    assertEquals("Second col: phonology layer pattern", "[aeiou].*", layerMatch.getPattern());
    assertNull("Second col: phonology layer min", layerMatch.getMin());
    assertNull("Second col: phonology layer max", layerMatch.getMax());
    assertNull("Second col: phonology layer target", layerMatch.getTarget());
    assertTrue("Second col: phonology layer anchorStart", layerMatch.getAnchorStart());
    assertNull("Second col: phonology layer anchorEnd", layerMatch.getAnchorEnd());

    layerMatch = col.getFirstLayerMatch("syllableCount");
    assertNotNull("Second col: syllableCount layer exists", layerMatch);
    assertEquals("Second col: syllableCount layer id", "syllableCount", layerMatch.getId());
    assertNull("Second col: syllableCount layer pattern", layerMatch.getPattern());
    assertEquals("Second col: syllableCount layer min", Double.valueOf(2.0), layerMatch.getMin());
    assertEquals("Second col: syllableCount layer max", Double.valueOf(3.0), layerMatch.getMax());
    assertNull("Second col: syllableCount layer target", layerMatch.getTarget());
    assertNull("Second col: syllableCount layer anchorStart", layerMatch.getAnchorStart());
    assertTrue("Second col: syllableCount layer anchorEnd", layerMatch.getAnchorEnd());

    layerMatch = col.getFirstLayerMatch("orthography");
    assertNotNull("Second col: orthography layer exists", layerMatch);
    assertEquals("Second col: orthography layer id", "orthography", layerMatch.getId());
    assertNull("Second col: orthography layer pattern", layerMatch.getPattern());
    assertNull("Second col: orthography layer min", layerMatch.getMin());
    assertNull("Second col: orthography layer max", layerMatch.getMax());
    assertTrue("Second col: orthography layer target", layerMatch.getTarget());
    assertNull("Second col: orthography layer anchorStart", layerMatch.getAnchorStart());
    assertNull("Second col: orthography layer anchorEnd", layerMatch.getAnchorEnd());
  }
  
  /** Ensure that (de)serialization of participant/transcript queries works. */
  @Test public void queries() {
    Matrix m = new Matrix()
      .addColumn(
        new Column().addLayerMatch(new LayerMatch().setId("orthography").setPattern("the")))
      .setParticipantQuery("first('participant_gender').label != 'M'")
      .setTranscriptQuery("first('transcript_type').label != 'wordlist'");

    assertEquals(
      "JSON serialization",
      "{\"columns\":[{\"adj\":1,\"layers\":"
      +"{\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"the\"}]}}],"
      +"\"participantQuery\":\"first('participant_gender').label != 'M'\","
      +"\"transcriptQuery\":\"first('transcript_type').label != 'wordlist'\""
      +"}",
      m.toString());    
    assertEquals("Description ignores queries", "orthography=the", m.getDescription());

    m = new Matrix().fromJsonString(m.toString());
    assertEquals("participantQuery deserialized",
                 "first('participant_gender').label != 'M'",
                 m.getParticipantQuery());
    assertEquals("transciptQuery deserialized",
                 "first('transcript_type').label != 'wordlist'",
                 m.getTranscriptQuery());    
  }
  
  /**
   * Ensure that deserialization from a legacy plain-text String works.
   * <p> Only a subset of original functionality has been migrated. In particular, border
   * conditions and anchoring aren't deserialized.
   */
  @Test public void fromLegacyString() {
    assertEquals("Single word search",
                 "{\"columns\":[{\"adj\":1,\"layers\":"
                 +"{\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"test\"}]}}]"
                 +"}",
                 new Matrix().fromLegacyString("test").toString());
    assertEquals("Multi word search",
                 "{\"columns\":["
                 +"{\"adj\":1,\"layers\":"
                 +"{\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"test\"}]}},"
                 +"{\"adj\":1,\"layers\":"
                 +"{\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"123\"}]}}"
                 +"]}",
                 new Matrix().fromLegacyString("test\t123").toString());
    assertEquals("Specify layer search",
                 "{\"columns\":[{\"adj\":1,\"layers\":"
                 +"{\"phonemes\":[{\"id\":\"phonemes\",\"pattern\":\"tEst\"}]}}]"
                 +"}",
                 new Matrix().fromLegacyString("phonemes:tEst").toString());
    assertEquals("Range search",
                 "{\"columns\":[{\"adj\":1,\"layers\":"
                 +"{\"syllableCount\":[{\"id\":\"syllableCount\",\"max\":3.0,\"min\":2.0}]}}]"
                 +"}",
                 new Matrix().fromLegacyString("syllableCount:2<3").toString());
    assertEquals("Range search - min only",
                 "{\"columns\":[{\"adj\":1,\"layers\":"
                 +"{\"syllableCount\":[{\"id\":\"syllableCount\",\"min\":2.0}]}}]"
                 +"}",
                 new Matrix().fromLegacyString("syllableCount:2<").toString());
    assertEquals("Range search - max only",
                 "{\"columns\":[{\"adj\":1,\"layers\":"
                 +"{\"syllableCount\":[{\"id\":\"syllableCount\",\"max\":3.0}]}}]"
                 +"}",
                 new Matrix().fromLegacyString("syllableCount:<3").toString());
    assertEquals("Multi-row search",
                 "{\"columns\":[{\"adj\":1,\"layers\":"
                 +"{\"phonemes\":[{\"id\":\"phonemes\",\"pattern\":\"s.*\"}],"
                 +"\"orthography\":[{\"id\":\"orthography\",\"pattern\":\"p.*\"}]}"
                 +"}]}",
                 new Matrix().fromLegacyString("phonemes:s.*\northography:p.*").toString());
    assertEquals("Multi-column search",
                 "{\"columns\":["
                 +"{\"adj\":1,\"layers\":"
                 +"{\"pos\":[{\"id\":\"pos\",\"pattern\":\"N\"}],"
                 +"\"phonemes\":[{\"id\":\"phonemes\",\"pattern\":\".*[aeiou]\"}]}},"
                 +"{\"adj\":1,\"layers\":"
                 +"{\"pos\":[{\"id\":\"pos\",\"pattern\":\"V\"}],"
                 +"\"phonemes\":[{\"id\":\"phonemes\",\"pattern\":\"[aeiou].*\"}]}}"
                 +"]}",
                 new Matrix().fromLegacyString(
                   "pos:N\nphonemes:.*[aeiou]\tpos:V\nphonemes:[aeiou].*").toString());
  }

  /**
   * Ensure that Matrix still function when not all attributes are present.
   */
  @Test public void incompleteMatrices() {
    Matrix m = (Matrix)
      (new Matrix().fromJsonString(
        "{\"columns\":[{\"layers\":{\"orthography\":{\"pattern\":\"test\"}}}]}"));
    assertEquals("Number of columns", 1, m.getColumns().size());
    Column col = m.getColumns().get(0);
    assertEquals("Col: adj", 1, col.getAdj());
    assertEquals("Col: number of layers", 1, col.getLayers().size());
    LayerMatch layerMatch = col.getFirstLayerMatch("orthography");
    assertNotNull("layer is orthography", layerMatch);
    assertEquals("layer id", "orthography", layerMatch.getId());
    assertNull("layer negation null", layerMatch.getNot());    
  }

  /**
   * Ensure target identification works for normal and edge cases.
   */
  @Test public void target() {
    Matrix m = new Matrix()
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("orthography").setPattern("the"))
                 .setAdj(3))
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("syllableCount").setMin(2.0).setMax(3.0)
                   .setAnchorEnd(true)
                   .setTarget(true))
                 .addLayerMatch(
                   new LayerMatch().setId("orthography").setPattern("")));
    assertEquals("Normal case - layer", "syllableCount", m.getTargetLayerId());
    assertEquals("Normal case - column", 1, m.getTargetColumn());

    m = new Matrix()
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("orthography").setPattern("the"))
                 .setAdj(3))
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("syllableCount").setMin(2.0).setMax(3.0)
                   .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("")));
    assertNull("No target - layer", m.getTargetLayerId());
    assertEquals("No target - column", -1, m.getTargetColumn());
    
    m = new Matrix()
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("orthography").setPattern("the"))
                 .setAdj(3))
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("syllableCount").setMin(2.0).setMax(3.0)
                   .setAnchorEnd(true))
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("")
                                .setTarget(true))); // target has no pattern
    assertNull("Target has no pattern - layer", m.getTargetLayerId());
    assertEquals("Target has no pattern - column", -1, m.getTargetColumn());
    
    m = new Matrix()
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("orthography").setPattern("the")
                   .setTarget(true)) // multiple targets
                 .setAdj(3))
      .addColumn(new Column().addLayerMatch(
                   new LayerMatch().setId("syllableCount").setMin(2.0).setMax(3.0)
                   .setAnchorEnd(true)
                   .setTarget(true)) // multiple targets                 
                 .addLayerMatch(new LayerMatch().setId("orthography").setPattern("")));
    // choose first target
    assertEquals("Multiple targets - layer", "orthography", m.getTargetLayerId());
    assertEquals("Multiple targets - column", 0, m.getTargetColumn());
    
  }

}
