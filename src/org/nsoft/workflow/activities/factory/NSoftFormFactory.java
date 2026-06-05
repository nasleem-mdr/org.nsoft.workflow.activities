package org.nsoft.workflow.activities.factory;

import org.adempiere.webui.factory.AnnotationBasedFormFactory;
import org.adempiere.webui.factory.IFormFactory;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IFormFactory.class, 
           property = {"service.ranking:Integer=10"})
public class NSoftFormFactory extends AnnotationBasedFormFactory {
    @Override
    protected String[] getPackages() {
        return new String[] {"org.adempiere.webui.apps.wf"};
    }
}