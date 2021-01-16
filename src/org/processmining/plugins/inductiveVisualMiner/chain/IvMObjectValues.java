package org.processmining.plugins.inductiveVisualMiner.chain;

import gnu.trove.map.hash.THashMap;

public class IvMObjectValues {

	private THashMap<IvMObject<?>, Object> object2value = new THashMap<>();

	@SuppressWarnings("unchecked")
	public <C> C get(IvMObject<C> name) {
		assert object2value.containsKey(name); //check whether the required object is present
		return (C) object2value.get(name);
	}

	public <C> void set(IvMObject<C> name, C object) {
		object2value.put(name, object);
	}

	public <C> IvMObjectValues s(IvMObject<C> name, C object) {
		set(name, object);
		return this;
	}

	public <C> boolean has(IvMObject<C> name) {
		return object2value.containsKey(name);
	}
}
