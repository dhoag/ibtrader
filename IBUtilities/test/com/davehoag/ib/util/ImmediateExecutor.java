package com.davehoag.ib.util;

import java.util.concurrent.Executor;

public class ImmediateExecutor implements Executor {

	@Override
	public void execute(Runnable arg0) {
		arg0.run();
	}

}
