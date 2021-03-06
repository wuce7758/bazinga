package org.bazinga.client.processor.consumer;

import io.netty.channel.Channel;

import java.util.concurrent.Executor;

import org.bazinga.client.executor.ConsumerExecutorFactory;
import org.bazinga.client.executor.ExecutorFactory;
import org.bazinga.client.processor.consumer.task.MessageTask;
import org.bazinga.common.logger.InternalLogger;
import org.bazinga.common.logger.InternalLoggerFactory;
import org.bazinga.common.message.Response;
import org.bazinga.common.spi.BazingaServiceLoader;

import static org.bazinga.common.utils.Constants.PROCESSOR_CORE_NUM_WORKERS;

/**
 * 消费者端的响应处理器
 * @author BazingaLyn
 * @copyright fjc
 * @time 2016年6月27日
 */
public class DefaultConsumerProcessor implements ConsumerProvider {
	
	private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultConsumerProcessor.class);
	
	private final Executor executor;
	
	public DefaultConsumerProcessor() {
        ExecutorFactory factory = (ExecutorFactory) BazingaServiceLoader.load(ConsumerExecutorFactory.class);
        executor = factory.newExecutor(PROCESSOR_CORE_NUM_WORKERS);
    }

	@Override
	public void handleResponse(Channel channel, Response response) throws Exception {
		MessageTask task = new MessageTask(channel, response);
        if (executor == null) {
        	logger.warn("use netty thread read response");
            task.run();
        } else {
            executor.execute(task);
        }
	}

}
