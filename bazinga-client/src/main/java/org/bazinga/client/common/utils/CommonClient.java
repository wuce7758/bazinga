package org.bazinga.client.common.utils;

import static org.bazinga.common.serialization.SerializerHolder.serializerImpl;

import org.bazinga.client.comsumer.DefaultConsumer;
import org.bazinga.client.dispatcher.DefaultDispatcher;
import org.bazinga.client.gather.DefaultResultGather;
import org.bazinga.client.preheater.ConectionPreHeater;
import org.bazinga.common.exception.NoServiceException;
import org.bazinga.common.group.BChannelGroup;
import org.bazinga.common.group.ServiceBChannelGroup;
import org.bazinga.common.group.ServiceBChannelGroup.CopyOnWriteGroupList;
import org.bazinga.common.message.Request;
import org.bazinga.common.message.RequestMessageWrapper;
import org.bazinga.common.message.SubScribeInfo;

public class CommonClient extends DefaultConsumer implements DefaultCommonClient {

	private final static long DEFAULT_TIME_OUT = 5000;
	
	private boolean preHeatStatus = true;

	public CommonClient(SubScribeInfo info) {
		super(info,-1,-1);
	}
	
	public CommonClient(SubScribeInfo info,int writeBufferHighWaterMark,int writeBufferLowWaterMark) {
		super(info,writeBufferHighWaterMark,writeBufferLowWaterMark);
	}

	public Object call(String serviceName, Object... args) throws Throwable {
		return call(serviceName, DEFAULT_TIME_OUT, args);

	}

	@Override
	public Object call(String serviceName, long timeout, Object... args) throws Throwable {

		ConectionPreHeater conectionPreHeater = new ConectionPreHeater(serviceName, timeout);
		
         if(preHeatStatus){
        	 conectionPreHeater.getPreHeatReady();
        	 preHeatStatus = false;
         }

		if (null == serviceName || serviceName.length() == 0) {
			throw new NoServiceException("调用的服务名不能为空");
		}
		BChannelGroup channelGroup = getAllMatchedChannel(serviceName);
		//因为预热了channel，此处应该不会发生异常，如果有异常，在预热阶段就应该抛出异常
		if (channelGroup.size() == 0) {
			throw new NoServiceException("没有第三方提供该服务，请检查服务名");
		}

		final Request request = new Request();
		RequestMessageWrapper message = new RequestMessageWrapper(serviceName, args);
		request.setMessageWrapper(message);
		request.bytes(serializerImpl().writeObject(message));
//		WeightChannel weightChannel = loadBalance(channels);

		DefaultResultGather defaultResultGather = new DefaultDispatcher().dispatcher(channelGroup.next(), request,timeout);

		return defaultResultGather.getResult();
	}

	private BChannelGroup getAllMatchedChannel(String serviceName) {
		CopyOnWriteGroupList groups = ServiceBChannelGroup.list(serviceName);
		return loadBalance(groups);
	}

}
