package org.nsoft.workflow.activities.factory;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.compiere.util.CLogger;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = IFormFactory.class,
           property = {"service.ranking:Integer=200"})
public class NSoftFormFactory implements IFormFactory {

    private static final CLogger log = CLogger.getCLogger(NSoftFormFactory.class);

    @Override
    public ADForm newFormInstance(String formId) {
       
        if ("org.nsoft.webui.apps.wf.WFActivity".equals(formId)) {
             return new org.nsoft.webui.apps.wf.WWFActivity();
        }
        
        return null;
    }
}