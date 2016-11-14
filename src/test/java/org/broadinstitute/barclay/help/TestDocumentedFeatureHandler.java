package org.broadinstitute.barclay.help;

import com.sun.javadoc.ClassDoc;

import java.util.*;

public class TestDocumentedFeatureHandler extends DefaultDocumentedFeatureHandler {

    @Override
    protected void addCustomBindings(final Class<?> classToProcess, final ClassDoc classDoc, final Map<String, Object> rootMap) {
        super.addCustomBindings(classToProcess, classDoc, rootMap);
        if (rootMap.get("testPlugin") == null) {
            rootMap.put("testPlugin", new HashSet<HashMap<String, Object>>());
        }
    }

    @Override
    protected String getTagFilterPrefix(){ return "MyTag"; }

}
