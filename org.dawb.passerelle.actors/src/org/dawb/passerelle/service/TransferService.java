package org.dawb.passerelle.service;

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.services.ITransferService;
import org.dawb.passerelle.actors.roi.ROISource;
import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

import uk.ac.diamond.scisoft.analysis.roi.ROIBase;

public class TransferService extends AbstractServiceFactory implements ITransferService {

	@Override
	public Object createROISource(String name, Object roi) throws Exception {
		final  List<ROISource> list = new ArrayList<ROISource>(1);// They force an ArrayList
		final ROISource source = ROISource.createSource(name, (ROIBase)roi);
		list.add(source);
		return list;
	}

	@Override
	public Object create(Class serviceInterface, IServiceLocator parentLocator, IServiceLocator locator) {
		if (serviceInterface==ITransferService.class) {
		    return new TransferService();
		}
		return null;
	}

}
