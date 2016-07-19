package org.wowtools.pmgowiki.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;

import org.apache.http.client.ClientProtocolException;

import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.conn.ClientConnectionManager;

import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * 功能说明：访问http形式的url，获得其html字符串
 * **/
public class HttpHelper {

	private DefaultHttpClient client;
	
	public DefaultHttpClient getClient() {
		return client;
	}
	public HttpHelper(){
		client = new DefaultHttpClient();
//		client.setRedirectHandler(new CustomRedirectHandler());
	}
	
	/**
	 * getCookies
	 * **/
	public List<Cookie> getCookies(){
		return client.getCookieStore().getCookies();
	}
	
	/**
	 * 加载SSL证书
	 * **/
	public void loadSSL() throws NoSuchAlgorithmException, KeyManagementException{
		X509TrustManager tm = new X509TrustManager(){
			public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}         
			public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}         
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			} 
		};
		SSLContext ctx = SSLContext.getInstance("TLS"); 
		ctx.init(null, new TrustManager[]{tm}, null); 
		SSLSocketFactory ssf = new SSLSocketFactory(ctx); 
		ClientConnectionManager ccm = client.getConnectionManager(); 
		SchemeRegistry sr = ccm.getSchemeRegistry(); 
		sr.register(new Scheme("https", 443, ssf)); 

	}
	
	/**使用get方式访问url，可用?在url后面添加参数
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws HttpException **/
	public String doGet(String strUrl,String[][] header) throws ClientProtocolException, IOException, URISyntaxException{
		String res = null;
		HttpGet httpGet = new HttpGet(strUrl);
		if(null!=header){
			for(String[] s:header){
				httpGet.setHeader(s[0],s[1]);
			}
		}
//		HttpHost proxyHost = new HttpHost("192.168.35.26", 808);//代理
//		httpGet.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);//设置代理
        HttpResponse response1 = client.execute(httpGet);
        try {
        	HttpEntity httpEntity = response1.getEntity();
            if(httpEntity != null){
                res = readHtmlContentFromEntity(httpEntity);
            }
            HttpEntity entity1 = response1.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity1);
        } finally {
            httpGet.releaseConnection();
        }
        return res;
	}
	
	/**使用get方式访问url,不返回html，可用?在url后面添加参数 **/
	public void doGetNoResult(String url) throws IOException{
		HttpGet httpGet = new HttpGet(url);
//		HttpHost proxyHost = new HttpHost("192.168.35.26", 808);//代理
//		httpGet.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);//设置代理
        client.execute(httpGet);
        httpGet.releaseConnection();
	}
	
	/**使用post方式访问url,
	 * @param 
	 * params 输入的参数params必须为二维数组params[n][2]。params[n][0]为参数名，params[n][1]为参数值 **/
	public String doPost(String url,String[][] params,String[][] header) throws IOException{
		String res = null;
		if(params[0].length!=2){
			System.out.println("输入的post参数格式不对");
			return null;
		}
		HttpPost httpPost = new HttpPost(url);
		if(null!=header){
			for(String[] s:header){
				httpPost.setHeader(s[0],s[1]);
			}
		}
//		HttpHost proxyHost = new HttpHost("192.168.35.26", 808);//代理
//		httpPost.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);//设置代理
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        for(String[] param:params){
        	nvps.add(new BasicNameValuePair(param[0],param[1]));
        }
        
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response2 = client.execute(httpPost);
        try {
        	HttpEntity httpEntity = response2.getEntity();
            if(httpEntity != null){
                res = readHtmlContentFromEntity(httpEntity);
            }
            HttpEntity entity2 = response2.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(entity2);
        } finally {
            httpPost.releaseConnection();
        }
        return res;
	}
	/**使用Ajax方式访问url,
	 * @param 
	 * params 输入的参数params必须为二维数组params[n][2]。params[n][0]为参数名，params[n][1]为参数值 **/
	public void doAjax(String url,String[][] params) throws IOException{
		if(params[0].length!=2){
			System.out.println("输入的post参数格式不对");
			return ;
		}
		HttpPost httpPost = new HttpPost(url);
//		HttpHost proxyHost = new HttpHost("192.168.35.26", 808);//代理
//		httpPost.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);//设置代理
		if(null!=params){
	        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
	        for(String[] param:params){
	        	nvps.add(new BasicNameValuePair(param[0],param[1]));
	        }
	        
	        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		}
//		httpPost.setHeader("x-ajaxpro-method", "GetDataSource");
//		httpPost.setHeader("Accept", "*/*");
//		httpPost.setHeader("Accept-Language", "zh-cn");
//		httpPost.setHeader("Accept-Encoding", "gzip, deflate");
//		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.setHeader("x-requested-with", "XMLHttpRequest");
//		List<Cookie> cookies = getCookies();
//		StringBuffer sbCookie = new StringBuffer();
//		for(Cookie cookie:cookies){
//			sbCookie.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
//		}
//		System.out.println(sbCookie);
//		httpPost.setHeader("Cookie", sbCookie.toString());
        client.execute(httpPost);
        httpPost.releaseConnection();
	}
	/**
     * 从response返回的实体中读取页面代码
     * @param httpEntity Http实体
     * @return 页面代码
     * @throws ParseException
     * @throws IOException
     */
    public static String readHtmlContentFromEntity(HttpEntity httpEntity) throws ParseException, IOException {
        String html = "";
        Header header = httpEntity.getContentEncoding();
        if(httpEntity.getContentLength() < 2147483647L){            //EntityUtils无法处理ContentLength超过2147483647L的Entity
            if(header != null && "gzip".equals(header.getValue())){
                html = EntityUtils.toString(new GzipDecompressingEntity(httpEntity));
            } else {
                html = EntityUtils.toString(httpEntity);
            }
        } else {
            InputStream in = httpEntity.getContent();
            if(header != null && "gzip".equals(header.getValue())){
                html = unZip(in, ContentType.getOrDefault(httpEntity).getCharset().toString());
            } else {
                html = readInStreamToString(in, ContentType.getOrDefault(httpEntity).getCharset().toString());
            }
            if(in != null){
                in.close();
            }
        }
        return html;
    }
    /**
     * 解压服务器返回的gzip流
     * @param in 抓取返回的InputStream流
     * @param charSet 页面内容编码
     * @return 页面内容的String格式
     * @throws IOException
     */
    private static String unZip(InputStream in, String charSet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream(in);
            byte[] _byte = new byte[1024];
            int len = 0;
            while ((len = gis.read(_byte)) != -1) {
                baos.write(_byte, 0, len);
            }
            String unzipString = new String(baos.toByteArray(), charSet);
            return unzipString;
        } finally {
            if (gis != null) {
                gis.close();
            }
            if(baos != null){
                baos.close();
            }
        }
    }
    /**
     * 读取InputStream流
     * @param in InputStream流
     * @return 从流中读取的String
     * @throws IOException
     */
    private static String readInStreamToString(InputStream in, String charSet) throws IOException {
        StringBuilder str = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, charSet));
        while((line = bufferedReader.readLine()) != null){
            str.append(line);
            str.append("\n");
        }
        if(bufferedReader != null) {
            bufferedReader.close();
        }
        return str.toString();
    }
    
}
