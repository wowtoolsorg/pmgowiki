package org.wowtools.pmgowiki.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Builder;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.ResponseEnvelop;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.ResponseEnvelop.Payload;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.ResponseEnvelop.Profile;
import org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.ResponseEnvelop.Unknown7;
import org.wowtools.pmgowiki.util.HttpHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

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
//	private static final String GOOGLEMAPS_KEY = "AIzaSyAZzeHhs-8JZ7i18MjFuM35dJHq70n3Hx4";
	private static final Long COORDS_LATITUDE = 0L;
	private static final Long COORDS_LONGITUDE = 0L;
	private static final Long COORDS_ALTITUDE = 0L;
//	private static final double FLOAT_LAT = 0;
//	private static final double FLOAT_LONG = 0;
	
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
	
	public ResponseEnvelop getProfile(String accessToken,String api,Unknown7 useauth,GeneratedMessage...reqq ){
		Builder req = PmgoProbuf.RequestEnvelop.newBuilder();
		org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder req1 = req.addRequestsBuilder();
		req1.setType(2);
		if(null!=reqq){
			int len = reqq.length;
			if(len>=1){
				req1.mergeFrom(reqq[0]);
				org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder req2 = req.addRequestsBuilder();
				req2.setType(126);
				if(len>=2){
					req2.mergeFrom(reqq[1]);
					org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder req3 = req.addRequestsBuilder();
					req3.setType(4);
					if(len>=3){
						req3.mergeFrom(reqq[2]);
						org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder req4 = req.addRequestsBuilder();
						req4.setType(129);
						if(len>=4){
						    req4.mergeFrom(reqq[3]);
							org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder req5 = req.addRequestsBuilder();
							req5.setType(5);
							if(len>=5){
								req5.mergeFrom(reqq[4]);
							}
						}
					}
				}
			}
		}
		return retryingApiReq(api, accessToken, new Builder[]{req}, true);
	} 
	
	public ResponseEnvelop retryingApiReq(String apiEndpoint,String accessToken, Builder[]args , boolean kwargs){
	        
	    while(true){
	    	try {
	    		ResponseEnvelop response = apiReq(apiEndpoint, accessToken, args, kwargs);
				if(null!=response){
					return response;
				}
			} catch (Exception e) {
				log("retrying_api_req: request error:"+e.getMessage());
				e.printStackTrace();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
				}
			}
	    }
	}
	
	public ResponseEnvelop apiReq(String apiEndpoint,String accessToken, Builder[]args, boolean kwargs){
		Builder pReq = PmgoProbuf.RequestEnvelop.newBuilder();
		pReq.setRpcId(1469378659230941192L);
		pReq.setUnknown1(2);
		pReq.setLatitude(COORDS_LATITUDE);
		pReq.setLongitude(COORDS_LONGITUDE);
		pReq.setAltitude(COORDS_ALTITUDE);
		pReq.setUnknown12(989);
		//TODO if 'useauth' not in kwargs or not kwargs['useauth']:
		org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.AuthInfo.Builder authBuilder = pReq.getAuthBuilder();
		authBuilder.setProvider("ptc");
		authBuilder.getTokenBuilder().setContents(accessToken);
		authBuilder.getTokenBuilder().setUnknown13(14);
		for(Builder bd:args){
			if(!bd.hasUnknown1()){
				bd.setUnknown1(2);
			}
			pReq.mergeFrom(bd.build());
		}

		HttpPost httpPost = new HttpPost(apiEndpoint);
		try {
			HttpEntity entity = new ByteArrayEntity(pReq.build().toByteArray());
			httpPost.setEntity(entity);
			CloseableHttpResponse r = httpHelper.getClient().execute(httpPost);
			InputStream c = r.getEntity().getContent();
			LinkedList<Integer> cList= new LinkedList<Integer>();
			int i = c.read();
			while(i>=0){
				cList.add(i);
				i = c.read();
			}
			byte[] bs = new byte[cList.size()];
			i = 0;
			for(int ii:cList){
				bs[i] = (byte) ii;
				i++;
			}
			ResponseEnvelop pRet = ResponseEnvelop.parseFrom(bs);
			return pRet;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	    
	
	public String getApiEndpoint(String accessToken,String api){
		if(null==api || api.length()==0){
			api = API_URL;
		}
		ResponseEnvelop profileResponse = null;
		while(profileResponse==null || !profileResponse.hasApiUrl()){
			 log("get_api_endpoint: calling get_profile");
			 profileResponse = getProfile(accessToken,api,null);
		}
		return "https://"+profileResponse.getApiUrl()+"/rpc";
	}
	
	
	
	/**
	 * 心跳程序
	 * def get_heartbeat(api_endpoint, access_token, response):
    m4 = pokemon_pb2.RequestEnvelop.Requests()
    m = pokemon_pb2.RequestEnvelop.MessageSingleInt()
    m.f1 = int(time.time() * 1000)
    m4.message = m.SerializeToString()
    m5 = pokemon_pb2.RequestEnvelop.Requests()
    m = pokemon_pb2.RequestEnvelop.MessageSingleString()
    m.bytes = "05daf51635c82611d1aac95c0b051d3ec088a930"
    m5.message = m.SerializeToString()
    walk = sorted(getNeighbors())
    m1 = pokemon_pb2.RequestEnvelop.Requests()
    m1.type = 106
    m = pokemon_pb2.RequestEnvelop.MessageQuad()
    m.f1 = ''.join(map(encode, walk))
    m.f2 = "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
    m.lat = COORDS_LATITUDE
    m.long = COORDS_LONGITUDE
    m1.message = m.SerializeToString()
    response = get_profile(
        access_token,
        api_endpoint,
        response.unknown7,
        m1,
        pokemon_pb2.RequestEnvelop.Requests(),
        m4,
        pokemon_pb2.RequestEnvelop.Requests(),
        m5)
    if response is None:
        return
    payload = response.payload[0]
    heartbeat = pokemon_pb2.ResponseEnvelop.HeartbeatPayload()
    heartbeat.ParseFromString(payload)
    return heartbeat
	 */
	public void heartbeat(String apiEndpoint,String accessToken,ResponseEnvelop profileResponse){
		org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder m4 = PmgoProbuf.RequestEnvelop.Requests.newBuilder();
		org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Unknown3.Builder m = PmgoProbuf.RequestEnvelop.Unknown3.newBuilder();
		m.setUnknown4(String.valueOf(System.currentTimeMillis()));//TODO m.f1 = int(time.time() * 1000)
		m4.setMessage(m.build());
		org.wowtools.pmgowiki.extractor.probuf.PmgoProbuf.RequestEnvelop.Requests.Builder m5 = PmgoProbuf.RequestEnvelop.Requests.newBuilder();
	}

	
	public static void main(String[] args) throws Exception {
		Extractor extractor = new Extractor();
		String accessToken = extractor.loginWithPTC("test1234820142", "test1234820142");
		log("accessToken "+accessToken);
		String apiEndpoint = extractor.getApiEndpoint(accessToken, null);
		log("apiEndpoint:"+apiEndpoint);
		
		ResponseEnvelop profileResponse = extractor.getProfile(accessToken, apiEndpoint, null, null);
		log("profileResponse :"+profileResponse);
		if(null==profileResponse){
			log("[-] Ooops...");
			throw new RuntimeException("profileResponse is null");
		}
		log("[+] Login successful");
		Payload payload = profileResponse.getPayload(0);
		Profile profile = payload.getProfile();
		String uname = profile.getUsername();
		log("username:"+uname);
		Date date = new Date(profile.getCreationTime());
		log("login time:"+date);
		extractor.heartbeat(apiEndpoint, accessToken, profileResponse);
	}
	
	private static void log(Object log){
		System.out.println(log);
	}
}
