package web;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;

import threading.FixedThreadPool;



import com.google.common.base.Optional;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;

import database.CsvKidneyDataBase;
import database.KidneyDataBase;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO(ross): make a flag
		int numThreads = 2;
		
		Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		KidneyDataBase database = new CsvKidneyDataBase();
		KidneyServerSolver solver = new KidneyServerSolver(database,threadPool);
		JsonKidneyServer jsonServer = new JsonKidneyServer(logger,solver,database);
		

	    Dispatcher dispatcher = new Dispatcher();
	    dispatcher.register(jsonServer);

	    int port = 8080;
	    if (args.length == 2) {
	      port = Integer.parseInt(args[1]);
	    }

	    logger.info("Listening on port " + port);
	    Server server = new Server(port);
	    
	    

	    ResourceHandler resource_handler = new ResourceHandler();
	    resource_handler.setDirectoriesListed(true);
	    resource_handler.setWelcomeFiles(new String[]{ "index.html" });

	    // TODO(rander) this needs to be a flag
	    //resource_handler.setResourceBase("/home/ross/src/kidneyWebsite/");
	    resource_handler.setResourceBase("webpage/");
	    
	    ContextHandlerCollection contexts = new ContextHandlerCollection();
	    contexts.addContext("/static", "").setHandler(resource_handler);
	    ContextHandler kidneyWrapper = contexts.addContext("/kidney", "");
	    kidneyWrapper.setHandler(new KidneyHandler(dispatcher));
	    server.setHandler(contexts);

	    try {
			server.start();
			server.join();
		} catch (Exception e) {
			logger.warning("server failed to start: " + e.getMessage());
			throw new RuntimeException(e);
		}
	    
	    
	    logger.info("Server terminating.");
	}

}
