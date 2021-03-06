package net.yoojia.asynchttp.support;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.yoojia.asynchttp.AsyncHttpConnection;
import net.yoojia.asynchttp.support.ParamsWrapper.NameValue;
import net.yoojia.asynchttp.support.ParamsWrapper.PathParam;
import net.yoojia.asynchttp.utility.MIMEType;


/**
 * @author : 桥下一粒砂
 * @email  : chenyoca@gmail.com
 * @date   : 2012-10-23
 * @desc   : 简单的HTTP实现
 */
public class SimpleHttpInvoker extends RequestInvoker {
	
	public final static String DEFAULT_USER_AGENT = String.format("AsyncHttpConnection (chenyoca@gmail.com) version %s", AsyncHttpConnection.VERSION);
	
	private final static int CONNECT_TIMEOUT = 15 * 1000;
	
	private static final String BOUNDARY = randomBoundry();
	private static final String MP_BOUNDARY = "--" + BOUNDARY;
	private static final String END_MP_BOUNDARY = "--" + BOUNDARY + "--";
	private static final String END_MP_BLOCK = "\r\n\r\n";
	private static final String MULTIPART_FORM_DATA = "multipart/form-data";
	private static final String XWWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	@Override
	protected void executing() {
		HttpURLConnection httpConnection = null;
		try{
			URL targetURL = null;
			String urlPath = url;
			InputStream stream = null;
			try{
				final boolean isGetMethod = HttpMethod.GET.equals(method) && params != null;
				if(isGetMethod){
					String strParam = params.getStringParams();
					if(strParam != null) urlPath = url + "?" + strParam;
				}
				targetURL = new URL(urlPath);
				httpConnection  = (HttpURLConnection) targetURL.openConnection();
				httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
				httpConnection.setDoInput(true);
				httpConnection.setUseCaches( HttpMethod.GET.equals(method) );
				httpConnection.setRequestMethod(method.name());
				httpConnection.setRequestProperty("User-agent",DEFAULT_USER_AGENT);
				if( !isGetMethod ){
					setParams(httpConnection, params);
				}
				callback.onSubmit(targetURL, params);
				httpConnection.connect();
				stream = httpConnection.getInputStream();
			}catch(IOException exp){
				callback.onError(exp);
			}	
			if(token != null){
				callback.onResponseWithToken(stream,targetURL,token);
			}else{
				callback.onResponse(stream,targetURL);
			}
		}finally{
			if(httpConnection != null) httpConnection.disconnect();
		}
	}
	
	public static void setParams(final HttpURLConnection conn, ParamsWrapper params) throws IOException {
		if(params == null) return;
		conn.setDoOutput(true);
		if(params.pathParamArray.isEmpty()){
			conn.setRequestProperty("Content-Type", XWWW_FORM_URLENCODED + "; boundary=" + BOUNDARY);
			DataOutputStream paramsOutStream = new DataOutputStream(conn.getOutputStream());
			paramsOutStream.write(params.getStringParams().getBytes());
			paramsOutStream.close();
		}else{
			conn.setRequestProperty("Connection:"," keep-alive");
			conn.setRequestProperty("Content-Type", MULTIPART_FORM_DATA + "; boundary=" + BOUNDARY);
			DataOutputStream paramsOutStream = new DataOutputStream(conn.getOutputStream());
			final int nameValueCount = params.nameValueArray.size();
			for(int i=0;i<nameValueCount;i++) {
				NameValue param = params.nameValueArray.get(i);
				setStringParamForPost(paramsOutStream, param.name, param.value);
			}
			final int pathParamCount = params.pathParamArray.size();
			for(int i=0;i<pathParamCount;i++) {
				PathParam param = params.pathParamArray.get(i);
				setPathParamForPost(paramsOutStream, param.param.name, param.param.value, param.path);
			}
			paramsOutStream.flush();
			paramsOutStream.close();
		}
	}
	
	public static void setStringParamForPost(OutputStream out,String paramName,String value) throws IOException{
		StringBuilder buffer = new StringBuilder();
		buffer.append(MP_BOUNDARY).append("\r\n");
		buffer.append("Content-Disposition: form-data; name=\"").append(paramName).append("\"").append(END_MP_BLOCK);
		buffer.append(value).append("\r\n");
		byte[] res = buffer.toString().getBytes();
		out.write(res);
	}
	
	public static void setPathParamForPost(OutputStream out, String paramName,String fileName,String filePath) throws IOException{
		StringBuilder buffer = new StringBuilder();
		buffer.append(MP_BOUNDARY).append("\r\n");
		buffer.append("Content-Disposition: form-data; name=\"").append(paramName).append("\"; ")
			.append("filename=\"").append(fileName).append("\"\r\n");
		final String contentType = MIMEType.getContentType(exportSuffix(filePath));
		buffer.append("Content-Type: ").append(contentType).append(END_MP_BLOCK);
		byte[] resourceSplitLine = buffer.toString().getBytes();
		FileInputStream input = null;
		try {
			out.write(resourceSplitLine);
			input = new FileInputStream(filePath);
			byte[] byteBuffer = new byte[10 * 1024];
			int EOF_SIZE = 0;
			while( (EOF_SIZE = input.read(byteBuffer)) != -1 ){
				out.write(byteBuffer, 0, EOF_SIZE);
			}
			out.write(END_MP_BLOCK.getBytes());
			out.write(END_MP_BOUNDARY.getBytes());
		}finally {
			if (null != input) {
				input.close();
			}
		}
	}
	
	private static String exportSuffix(String path){
		return path.substring(path.lastIndexOf(".") + 1);
	}
	
	static String randomBoundry() {
        StringBuffer buffer = new StringBuffer("----BoundaryGenByInvoker");
        for (int t = 1; t < 12; t++) {
            long time = System.currentTimeMillis() + t;
            if (time % 3 == 0) {
                buffer.append((char) time % 9);
            } else if (time % 3 == 1) {
                buffer.append((char) (65 + time % 26));
            } else {
                buffer.append((char) (97 + time % 26));
            }
        }
        return buffer.toString();
    }

}
