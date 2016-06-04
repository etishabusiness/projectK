package com.etisha.projectk.restclient;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import com.xeeva.core.authentication.UserVO;
import com.xeeva.core.request.CommonConstants;
import com.xeeva.core.request.DataVO;
import com.xeeva.core.request.GenericData;
import com.xeeva.core.request.RequestVO;
import com.xeeva.core.request.Response;

@Slf4j
/**
 * This class is use to call web Services.
 * 
 * 
 * 
 * @author Kaushlendra Dixit
 *
 */
public class StandardRestTemplate {
	
	
	private static final StandardRestTemplate template= new StandardRestTemplate();
	
	private RestTemplate restTemplate;
	
	

	/**
	 * To create instance of XeevaRestTemplate
	 * @author Kaushlendra Dixit
	 * @return
	 */
	public static StandardRestTemplate getInstance() {
		return template;
	}
	
	
		
	/**
	 * Constructor initialize the spring rest template
	 * @author Kaushlendra Dixit
	 */
	private StandardRestTemplate() {
		restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
	}
	/**
	 * 
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @return
	 */
	public Collection<? extends Object> postForList(String url,Map<String,Object> requestParams) {
		return postForList(url,requestParams,-1,-1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * 
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @param paginationOffSet
	 * @param maxRecords
	 * @return
	 */
	public Collection<? extends Object> postForList(String url,Map<String,Object> requestParams,int paginationOffSet,int maxRecords) {
		
		Response response = call(url, requestParams, paginationOffSet, maxRecords);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			List list = (List) objectMapper.readValue(
							objectMapper.writeValueAsString(response.getData()),
							new TypeReference<List>() {
							});
			log.debug("List elements are :: " + list);
			return list;
		} catch (Exception e) {
			log.error("Error in extracting categories", e);
		}
		
		return null;
	}
	
	/**
	 *  This method calls the webservice of a given url and returns map.
	 * @param url
	 * @param requestParams
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Map postForMap(String url,Map<String,Object> requestParams) {
		return postForMap(url,requestParams,-1,-1);
	}
	
	@SuppressWarnings({ "rawtypes" })
	/**
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @param paginationOffSet
	 * @param maxRecords
	 * @return
	 */
	public Map postForMap(String url,Map<String,Object> requestParams,int paginationOffSet,int maxRecords) {
		
		Response response = call(url, requestParams, paginationOffSet, maxRecords);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Map map = (Map) objectMapper.readValue(
							objectMapper.writeValueAsString(response.getData()),
							new TypeReference<Map>() {
							});
			log.debug("Map elements are :: " + map);
			return map;
		} catch (Exception e) {
			log.error("Error in extracting categories", e);
		}
		
		return null;
	}


  

	
	@SuppressWarnings("rawtypes")
	/**
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @param clazz
	 * @param paginationOffSet
	 * @param maxRecords
	 * @return
	 */
	public Object postForObject(String url,Map<String,Object> requestParams, Class clazz, int paginationOffSet,int maxRecords) {
		Response response = call(url, requestParams, paginationOffSet, maxRecords);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			@SuppressWarnings({ "unchecked" })
			Object result =  objectMapper.readValue(objectMapper.writeValueAsString(response.getData()),clazz);
			
			log.debug("Result is  :: " + result);
			return result;
		} catch (Exception e) {
			log.error("Error in extracting categories", e);
		}
		
		return null;
	}
	
	
	@SuppressWarnings("rawtypes")
	/**
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @param clazz
	 * @return
	 */
	public Object postForObject(String url,Map<String,Object> requestParams, Class clazz) {
		return postForObject(url,requestParams,clazz,-1,-1);
	}
	/**
	 * @author Kaushlendra Dixit
	 * @param requestObj
	 * @param url
	 * @return
	 */
	public Response post(DataVO requestObj, String url) {
		Response response = null;
		log.info("calling ....." + url);
		response = restTemplate.postForObject(url, requestObj,Response.class);
		log.info("Response   " + url + " :: " + response);

		return handleResponse(response);
	}
	
	/**
	 * @author Kaushlendra Dixit
	 * @param response
	 * @return
	 */
	private Response handleResponse(Response response) {
		log.debug("Response from server " + response);
		if (response.getResponseCode().contains(CommonConstants.SUCCESS)) {
			log.debug("Success response from Server");
			return response;

		} else if (response.getResponseCode().contains(CommonConstants.NOT_YET_DEVELOPED)) {
			throw new NotYetDevelopedException(CommonConstants.NOT_YET_DEVELOPED);
		} else {
			throw new FailedResponseException(response.getData()!=null?response.getData().toString():"");
		}
	}


	
	  /**
     * 
     * @param url
     * @param requestParams
     * @param paginationOffSet
     * @param maxRecords
     * @return
     */
	private Response call(String url, Map<String, Object> requestParams, int paginationOffSet, int maxRecords) {
		RequestVO request = getRequestVO(requestParams, paginationOffSet,maxRecords);

		log.info(String.format("Calling webservice [%s]  for the request params %s", url,requestParams));

		Response response = restTemplate.postForObject(url, request,Response.class);
		log.info(String.format("Response back from  webservice [%s]  for the request params %s  is \n [%s]",	url, requestParams, response));
		response=handleResponse(response);
		return response;
	}
	
	
	
		
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * 
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @param paginationOffSet
	 * @param maxRecords
	 * @return
	 */
	public Collection<GenericData> postForGenericDataList(String url,Map<String,Object> requestParams,int paginationOffSet,int maxRecords) {
		
		Response response = call(url, requestParams, paginationOffSet, maxRecords);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			List<GenericData> list = (List<GenericData>) objectMapper.readValue(
							objectMapper.writeValueAsString(response.getData()),
							new TypeReference<List<GenericData>>() {
							});
			log.debug("List elements are :: " + list);
			return list;
		} catch (Exception e) {
			log.error("Error in extracting categories", e);
		}
		
		return null;
	}
	
	/**
	 * 
	 * @author Kaushlendra Dixit
	 * @param url
	 * @param requestParams
	 * @param paginationOffSet
	 * @param maxRecords
	 * @return
	 */
	public Collection<GenericData> postForGenericDataList(String url,Map<String,Object> requestParams) {
		
		return postForGenericDataList(url,requestParams,-1,-1);
	}
	
}	


