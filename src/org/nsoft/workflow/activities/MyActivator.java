/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 **********************************************************************/
package org.nsoft.workflow.activities;

import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.adempiere.webui.Extensions;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.compiere.util.CLogger;

@Component(immediate = true)
public class MyActivator extends Incremental2PackActivator {

	public MyActivator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
	    CLogger.getCLogger(MyActivator.class)
	        .warning("=== NSoft WFActivity bundle START ==="
	                + " | bundle=" + context.getBundle().getSymbolicName()
	                + " | version=" + context.getBundle().getVersion());
	    super.start(context);
	}

}
