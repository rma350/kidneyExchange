package web;

import ilog.concert.IloException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import web.JsonRpcConstants;

import com.google.common.base.Splitter;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

import database.KidneyDataBase;

public class JsonKidneyServer implements RequestHandler{
	
	private Logger logger;
	private KidneyServerSolver solver;
	private KidneyDataBase kidneyDataBase;
	
	public JsonKidneyServer(Logger logger,KidneyServerSolver solver, KidneyDataBase kidneyDatabase){
		this.logger = logger;
		this.solver = solver;
		this.kidneyDataBase = kidneyDatabase;
	}

	@Override
	public String[] handledRequests() {
		logger.log(Level.INFO, "methods queried");
		return new String[]{JsonRpcConstants.METHOD_NAME_LOAD_DATA, 
				JsonRpcConstants.METHOD_NAME_SOLVE, JsonRpcConstants.METHOD_NAME_DATASETS};
	}

	@Override
	public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
		StringBuilder logBuilder = new StringBuilder()
        .append(request.getMethod())
        .append(" request params")
        .append(request.getParams().toString())
        .append(" from ")
        .append(context.getClientInetAddress())
        .append(": ");
		if (request.getMethod().equals(JsonRpcConstants.METHOD_NAME_LOAD_DATA)) {
		      logger.info(logBuilder.append("dispatched.").toString());
		      return handleLoadData(request,context);
		}
		else if (request.getMethod().equals(JsonRpcConstants.METHOD_NAME_DATASETS)) {
		      logger.info(logBuilder.append("dispatched.").toString());
		      return handleDatasets(request,context);
		}
		else if (request.getMethod().equals(JsonRpcConstants.METHOD_NAME_SOLVE)) {
		      logger.info(logBuilder.append("dispatched.").toString());
		      return handleSolve(request,context);
		}
		else{
			logger.info(logBuilder.append("invalid.").toString());
		    return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND,
		                                request.getID());
		}		
	}
	
	private JSONRPC2Response handleDatasets(JSONRPC2Request request, MessageContext context){
		return new JSONRPC2Response(kidneyDataBase.availableDatasets(),request.getID());
	}
	
	private JSONRPC2Response handleLoadData(JSONRPC2Request request, MessageContext context){
		if (!request.getParamsType().equals(JSONRPC2ParamsType.OBJECT) ||
				!(request.getParams() instanceof Map)) {
			return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS,
					request.getID());
		}
		Map<?,?> map = (Map<?,?>) request.getParams();
		if (!map.containsKey(JsonRpcConstants.PARAM_NAME_DATASET)) {
			return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS,
					request.getID());
		}
		String datasetName = getArg(map, JsonRpcConstants.PARAM_NAME_DATASET);
		if (datasetName == null) {
			return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS,
					request.getID());
		}
		datasetName = datasetName.trim();
		JSONRPC2Response response = new JSONRPC2Response(solver.getInputs(datasetName),request.getID());

		System.out.println("reponse: " + datasetName);
		return response;

	}
	
	private static String getArg(Map args, String arg) {
	    if (args.containsKey(arg) && args.get(arg) instanceof String) {
	      return (String) args.get(arg);
	    }
	    return null;
	  }
	
	private JSONRPC2Response handleSolve(JSONRPC2Request request, MessageContext context){
		if (!request.getParamsType().equals(JSONRPC2ParamsType.OBJECT) ||
				!(request.getParams() instanceof Map)) {
			return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS,
					request.getID());
		}
		Map<?,?> map = (Map<?,?>) request.getParams();
		if (!map.containsKey(JsonRpcConstants.PARAM_NAME_DATASET)) {
			return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS,
					request.getID());
		}
		String datasetName = getArg(map, JsonRpcConstants.PARAM_NAME_DATASET).trim();
		if (datasetName == null) {
			return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS,
					request.getID());
		}
		datasetName = datasetName.trim();
		JSONRPC2Response response;
		try {
			response = new JSONRPC2Response(solver.getSolution(datasetName),request.getID());
		} catch (IloException e) {
			System.err.println(e);
			System.err.println(e.getStackTrace());
			return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR,
					request.getID());
		}
		System.out.println("reponse: " + response);
		return response;
	}

	

}
