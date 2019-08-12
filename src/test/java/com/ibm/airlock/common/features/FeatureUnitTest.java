package com.ibm.airlock.common.features;

import com.ibm.airlock.common.model.Feature;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Denis Voloshin
 */

public class FeatureUnitTest {

    @Test
    public void setNullChildTest() {
        Feature f = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        //Add a valid child to have a non null children list
        Feature c = new Feature("ThreeUp.V1.c", true, Feature.Source.DEFAULT);
        f.addUpdateChild(c);
        Assert.assertTrue("NULL children list was returned after adding a child", f.getChildren() != null);
        f.addUpdateChild(null);
        Assert.assertTrue("NULL value should not be added to the children list", !f.getChildren().contains(null));
    }

    @Test
    public void setNullChildToNullListTest() {
        Feature f = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        f.addUpdateChild(null);
        Assert.assertTrue("NULL value should not be added to the children list", f.getChildren().isEmpty());
    }

    @Test
    public void removeChildTest() {
        Feature f = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        //Add a valid child to have a non null children list
        Feature c = new Feature("ThreeUp.V1.c", true, Feature.Source.DEFAULT);
        f.addUpdateChild(c);
        Assert.assertTrue("NULL children list was returned after adding a child", f.getChildren() != null);
        Assert.assertTrue("A list of size = 1 is expected after adding a child", f.getChildren().size() == 1);
        Assert.assertTrue("The children list should contain the child we just added.", f.getChildren().get(0).getName().equalsIgnoreCase("ThreeUp.V1.c"));
        f.removeChild(c);
        Assert.assertTrue("NULL children list was returned after adding and removing a child", f.getChildren() != null);
        Assert.assertTrue("A list of size = 0 is expected after adding a child", f.getChildren().size() == 0);
        Assert.assertTrue("The children list should not contain the child we just removed.", !f.getChildren().contains(c));
    }

    @Test
    public void removeNullChildTest() {
        Feature f = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        //Add a valid child to have a non null children list
        Feature c = new Feature("ThreeUp.V1.c", true, Feature.Source.DEFAULT);
        f.addUpdateChild(c);
        Assert.assertTrue("NULL children list was returned after adding a child", f.getChildren() != null);
        Assert.assertTrue("A list of size = 1 is expected after adding a child", f.getChildren().size() == 1);
        Assert.assertTrue("The children list should contain the child we just added.", f.getChildren().get(0).getName().equalsIgnoreCase("ThreeUp.V1.c"));
        f.removeChild(null);
        Assert.assertTrue("NULL children list was returned after adding a child", f.getChildren() != null);
        Assert.assertTrue("A list of size = 1 is expected after removing a null child", f.getChildren().size() == 1);
        Assert.assertTrue("The children list should contain the child we just added after trying to remove null.", f.getChildren().get(0).getName().equalsIgnoreCase("ThreeUp.V1.c"));
    }

    @Test
    public void toStringTest() {
        Feature f = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        try {
            JSONObject toJson = new JSONObject(f.toString());
        } catch (JSONException e) {
            Assert.fail("toString format should have a valid json format. Message: " + e.getMessage());
        }
    }

    @Test
    public void stringConstructorTest() {
        Feature f = new Feature("{\"configuration\":{},\"isON\":true,\"fullName\":\"ThreeUp.V1\",\"source\":\"DEFAULT\"}");
        Assert.assertTrue(f.getName().equalsIgnoreCase("ThreeUp.V1"));
        Assert.assertTrue(f.getSource().equals(Feature.Source.DEFAULT));
        Assert.assertTrue(f.isOn());
        Assert.assertTrue(f.getConfiguration().toString().equals("{}"));
    }
}
