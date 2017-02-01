package org.broadinstitute.barclay.help;

import java.util.*;

public class TestDocWorkUnitHandler extends DefaultDocWorkUnitHandler {

    public TestDocWorkUnitHandler(final HelpDoclet doclet) {
        super(doclet);
    }
    @Override
    protected void addCustomBindings(final DocWorkUnit currentworkUnit) {
        super.addCustomBindings(currentworkUnit);
        if (currentworkUnit.getProperty("testPlugin") == null) {
            currentworkUnit.setProperty("testPlugin", new HashSet<HashMap<String, Object>>());
        }
    }

    @Override
    protected String getTagFilterPrefix(){ return "MyTag"; }

}
