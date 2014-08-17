package web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Message;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

public class KidneyHandler extends AbstractHandler {

	private final Dispatcher rpcDispatcher;

	  public KidneyHandler(Dispatcher rpcDispatcher) {
	    this.rpcDispatcher = rpcDispatcher;
	  }

	  private static String getRequestBody(HttpServletRequest req)
	      throws IOException {
	    BufferedReader reader = null;
	    try {
	      reader = new BufferedReader(
	          new InputStreamReader(req.getInputStream(), "UTF-8"));
	    } catch (UnsupportedEncodingException exception) {
	      throw new RuntimeException("Use UTF...",exception);
	    }

	    Writer writer = new StringWriter();

	    char[] readBuffer = new char[2048];
	    int bytesRead = reader.read(readBuffer);
	    while (bytesRead > 0) {
	      writer.write(readBuffer, 0, bytesRead);
	      bytesRead = reader.read(readBuffer);
	    }
	    return writer.toString();
	  }

	  public void handle(String target, Request baseRequest,
	                     HttpServletRequest req, HttpServletResponse resp)
	      throws IOException, ServletException {
	    resp.setContentType("application/json-rpc");

	    MessageContext messageContext = new MessageContext(req);
	    JSONRPC2Request request = null;
	    try {
	      JSONRPC2Message jsonMessage = JSONRPC2Message.parse(getRequestBody(req));
	      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	      if (!(jsonMessage instanceof JSONRPC2Request)) {
	        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	        return;
	      }
	      request = (JSONRPC2Request) jsonMessage;
	    } catch (JSONRPC2ParseException e) {
	      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	      return;
	    }
	    resp.setStatus(HttpServletResponse.SC_OK);

	    String responseJson =
	        rpcDispatcher.dispatch(request, messageContext).toString();
	    resp.setContentLength(responseJson.length());
	    System.err.println("response json: " + responseJson);
	    resp.getWriter().print(responseJson);
	  }

}
