package managers;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import data.Log;
import utils.Constants;

public class ThreadingManager {
	
	private static ThreadingManager instance;
	private ExecutorService m_threadPool;
	private boolean m_shuttingDown;
	
	public static ThreadingManager getInstance() {
		if(instance == null) instance = new ThreadingManager();
		
		return instance;
	}
	
	public ThreadingManager() {
		m_shuttingDown = false;
		m_threadPool = Executors.newFixedThreadPool(Constants.THREAD_POOL_SIZE);
	}
	
	public void executeSync(Runnable p_runnable, int p_timeout) {
		if(m_shuttingDown) return;
		
		Future<?> future = m_threadPool.submit(p_runnable);
		
		try {
			future.get(p_timeout, TimeUnit.MILLISECONDS);
			
			if(!future.isDone()) future.cancel(true);
		} catch(Exception e) {
			Log.log(Level.WARNING, "Could not wait on future", e);
		}
	}
	
	public <T> T executeSync(Callable<T> p_callable, int p_timeout) {
		if(m_shuttingDown) return null;
		
		Future<T> future = m_threadPool.submit(p_callable);
		
		try {
			T result = future.get(p_timeout, TimeUnit.MILLISECONDS);
			
			if(!future.isDone()) future.cancel(true);
			
			return result;
		} catch(Exception e) {
			Log.log(Level.WARNING, "Could not wait on future", e);
			
			return null;
		}
	}
	
	public void executeAsync(Runnable p_runnable, int p_timeout,
							 boolean p_interruptWhileRunning) {
		if(m_shuttingDown) return;
		
		Future<?> future = m_threadPool.submit(p_runnable);
		
		new Timer().schedule(new TimerTask() {
			public void run() {
				if(future != null && !future.isDone() && !future.isCancelled())
					future.cancel(p_interruptWhileRunning);
			}
		}, p_timeout > 0 ? p_timeout : 0);
	}
	
	public <T> Future<T> executeAsync(Callable<T> p_callable, int p_timeout, 
									  boolean p_interruptWhileRunning) {
		if(m_shuttingDown) return null;
		
		Future<T> future = m_threadPool.submit(p_callable);
		
		new Timer().schedule(new TimerTask() {
			public void run() {
				if(future != null && !future.isDone() && !future.isCancelled())
					future.cancel(p_interruptWhileRunning);
			}
		}, p_timeout > 0 ? p_timeout : 0);
		
		return future;
	}
	
	public void stop(int p_timeout) {
		m_shuttingDown = true;
		m_threadPool.shutdown();
		
		try {
			m_threadPool.awaitTermination(p_timeout, TimeUnit.MILLISECONDS);
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Error while awaiting thread pool termination", e);
		}
	}
}
