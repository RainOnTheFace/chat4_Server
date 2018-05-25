package com.cn.server;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cn.common.core.codc.RequestDecoder;
import com.cn.common.core.codc.ResponseEncoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * netty服务端入门
 *
 * @author -琴兽-
 *
 */
@Component
public class Server {
	public static Logger log = Logger.getLogger(Server.class);

	/**
	 * 启动
	 */
	public void start() {

		// 服务类
		ServerBootstrap b = new ServerBootstrap();

		// 创建boss和worker
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		// 业务线程池
		final EventLoopGroup executorService = new NioEventLoopGroup();

		try {
			// 设置循环线程组事例
			b.group(bossGroup, workerGroup);

			// 设置channel工厂
			b.channel(NioServerSocketChannel.class);

			// 设置管道
			b.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					//		ch.pipeline().addLast(new IdleStateHandler(5, 5, 10));
					ch.pipeline().addLast(new RequestDecoder());
					ch.pipeline().addLast(new ResponseEncoder());
					// ch.pipeline().addLast(new ServerHandler());
					// 这样ServerHandler中的处理就是另外一个异步线程去处理了。
					ch.pipeline().addLast(executorService, new ServerHandler());
				}
			});

			b.option(ChannelOption.SO_BACKLOG, 2048);// 链接缓冲池队列大小

			// 绑定端口
			b.bind(10102).sync();
			log.info("*****start");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
