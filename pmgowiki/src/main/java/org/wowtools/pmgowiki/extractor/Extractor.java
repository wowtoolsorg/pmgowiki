package org.wowtools.pmgowiki.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Builder;
import org.wowtools.pmgowiki.util.HttpHelper;

/**
 * 抓取服务器数据的工具类
 * 
 * @author liuyu
 * @date 2016年7月19日
 */
public class Extractor {
	private static final String API_URL = "https://pgorelease.nianticlabs.com/plfe/rpc";
	private static final String LOGIN_URL = "https://sso.pokemon.com/sso/login?service=https%3A%2F%2Fsso.pokemon.com%2Fsso%2Foauth2.0%2FcallbackAuthorize";
	private static final String LOGIN_OAUTH = "https://sso.pokemon.com/sso/oauth2.0/accessToken";
	private static final String PTC_CLIENT_SECRET = "w8ScCUXJQc6kXKw8FiOhd8Fixzht18Dq3PEVkUCP5ZPxtgyWsbTvWHFLm2wNY0JR";
	private static final String GOOGLEMAPS_KEY = "AIzaSyAZzeHhs-8JZ7i18MjFuM35dJHq70n3Hx4";

	
	private HttpHelper httpHelper = new HttpHelper();
	/**
	 * 用ptc账号登录
	 * 
	 * @param usr
	 * @param pwd
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public String loginWithPTC(String usr, String pwd) throws Exception {
		String[][] header = new String[][]{{"User-Agent","Niantic App"}};
		JSONObject step1Res = loginStep1(header);
		String ticket = loginStep2(header, usr, pwd, step1Res);
		String res = loginStep3(ticket);
		return res;

	}
	
	private JSONObject loginStep1(String[][] header) throws Exception{
		String res = httpHelper.doGet(LOGIN_URL, header);
		JSONObject jo = new JSONObject(res);
		String lt;
		try {
			lt = jo.getString("lt");
		} catch (Exception e) {
			throw new Exception("登录失败,step1:"+res);
		}
		if(null==lt || lt.length()==0){
			throw new Exception("登录失败,step1,空返回内容:"+res);
		}
		return jo;
	}
	
	private String loginStep2(String[][] header,String usr, String pwd,JSONObject step1Res) throws Exception{
		HttpPost httpPost = new HttpPost(LOGIN_URL);
		try {
			for(String[] s:header){
				httpPost.setHeader(s[0],s[1]);
			}
//		HttpHost proxyHost = new HttpHost("192.168.35.26", 808);//代理
//		httpPost.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);//设置代理
			List <NameValuePair> nvps = new ArrayList <NameValuePair>(5);
			nvps.add(new BasicNameValuePair("lt",step1Res.getString("lt")));
			nvps.add(new BasicNameValuePair("execution",step1Res.getString("execution")));
			nvps.add(new BasicNameValuePair("_eventId","submit"));
			nvps.add(new BasicNameValuePair("username",usr));
			nvps.add(new BasicNameValuePair("password",pwd));
			
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));
			HttpResponse response2 = httpHelper.getClient().execute(httpPost);
			Header hd = response2.getFirstHeader("Location");
			String res = hd.getValue();
			res = res.substring(res.lastIndexOf("ticket=")+7);//"ticket=".length=7
			return res;
		} catch (Exception e) {
			throw new Exception("登录失败,step2:",e);
		} finally {
            httpPost.releaseConnection();
        }
       
	}

	
	private String loginStep3(String ticket) throws Exception{
		try {
			String[][] params = new String[][]{
				{"client_id","mobile-app_pokemon-go"},
				{"redirect_uri","https://www.nianticlabs.com/pokemongo/error"},
				{"client_secret",PTC_CLIENT_SECRET},
				{"grant_type","refresh_token"},
				{"code",ticket},
			};
			String res = httpHelper.doPost(LOGIN_OAUTH, params, null);
			int b = res.indexOf("access_token=")+13;
			int e = res.indexOf("&",b);
			if(e<=0){
				e=res.length();
			}
			res = res.substring(b,e);
			return res;
		} catch (Exception e) {
			throw new Exception("登录失败,step3:",e);
		}
	}
	/**
	def get_profile(access_token, api, useauth, *reqq):
	    req = pokemon_pb2.RequestEnvelop()
	    req1 = req.requests.add()
	    req1.type = 2
	    if len(reqq) >= 1:
	        req1.MergeFrom(reqq[0])

	    req2 = req.requests.add()
	    req2.type = 126
	    if len(reqq) >= 2:
	        req2.MergeFrom(reqq[1])

	    req3 = req.requests.add()
	    req3.type = 4
	    if len(reqq) >= 3:
	        req3.MergeFrom(reqq[2])

	    req4 = req.requests.add()
	    req4.type = 129
	    if len(reqq) >= 4:
	        req4.MergeFrom(reqq[3])

	    req5 = req.requests.add()
	    req5.type = 5
	    if len(reqq) >= 5:
	        req5.MergeFrom(reqq[4])
	    return retrying_api_req(api, access_token, req, useauth=useauth)
	 */
	
	public org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.ResponseEnvelop.Builder getProfile(String accessToken,String api,String r){
		return null;
	} 
	
	public String getApiEndpoint(String accessToken,String api){
		if(null==api || api.length()==0){
			api = API_URL;
		}
		org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.ResponseEnvelop.Builder 	profileResponse = null;
		while(profileResponse==null || profileResponse.hasApiUrl()){
			 log("get_api_endpoint: calling get_profile");
			 profileResponse = getProfile(accessToken,api,null);
		}
		return "https://"+profileResponse.getApiUrl()+"/rpc";
	}

	
	public static void main(String[] args) throws Exception {
		Extractor extractor = new Extractor();
		String accessToken = extractor.loginWithPTC("test1234820142", "test1234820142");
		System.out.println("login success "+accessToken);
	}
	
	private void log(String log){
		System.out.println(log);
	}
}
