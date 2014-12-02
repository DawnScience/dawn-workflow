package org.dawb.passerelle.common.parameter;

public interface Marshaller<T> {

	String marshal(T value) throws Exception ;
	Object unmarshal(String str, Class<? extends T> clazz) throws Exception ;

}
