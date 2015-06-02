package org.dawb.passerelle.common;

import org.eclipse.dawnsci.analysis.api.IClassLoaderService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassLoaderServiceHolder {
	
	private static final Logger logger = LoggerFactory.getLogger(ClassLoaderServiceHolder.class);

	private IClassLoaderService classLoaderService;

	private static ClassLoaderServiceHolder current;
	
	public static ClassLoaderServiceHolder getInstance() {
		if (current==null) current = new ClassLoaderServiceHolder();
		return current;
	}
	public void start(ComponentContext context) {
		current = this;
	}
	
	public void stop() {
		current = null;
	}

	public IClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	public void setClassLoaderService(IClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
		logger.debug("Set "+classLoaderService.getClass().getSimpleName()+" in "+getClass().getSimpleName());
	}
}
