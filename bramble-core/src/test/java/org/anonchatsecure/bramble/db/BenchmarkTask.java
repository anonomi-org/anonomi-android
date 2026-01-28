package org.anonchatsecure.bramble.db;

interface BenchmarkTask<T> {

	void run(T context) throws Exception;
}
