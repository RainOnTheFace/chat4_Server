package com.cn.server;

import org.apache.log4j.Logger;

import com.cn.common.core.model.Request;
import com.cn.common.core.model.Response;
import com.cn.common.core.model.Result;
import com.cn.common.core.model.ResultCode;
import com.cn.common.core.serial.Serializer;
import com.cn.common.core.session.Session;
import com.cn.common.core.session.SessionImpl;
import com.cn.common.core.session.SessionManager;
import com.cn.common.module.ModuleId;
import com.cn.server.module.player.dao.entity.Player;
import com.cn.server.scanner.Invoker;
import com.cn.server.scanner.InvokerHoler;
import com.google.protobuf.GeneratedMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 消息接受处理类
 * 
 * @author -琴兽-
 *
 */
public class ServerHandler extends SimpleChannelInboundHandler<Request> {
	public static Logger log = Logger.getLogger(ServerHandler.class);

	// 业务线程池 netty 3
	// public static ExecutorService executorService = Executors
	// .newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
	/**
	 * 接收消息
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
		log.info("*****当前线程id: " + Thread.currentThread().getId());
		Response response = new Response(request);
		// 把当前通道抽象成自定义的session
		Session session = new SessionImpl(ctx.channel());

		log.info("****** module:" + request.getModule() + "   " + "cmd：" + request.getCmd());

		// 获取命令执行器
		Invoker invoker = InvokerHoler.getInvoker(request.getModule(), request.getCmd());
		if (invoker != null) {
			try {
				Result<?> result = null;
				// 假如是玩家模块传入channel参数，否则传入playerId参数
				if (request.getModule() == ModuleId.PLAYER) {
					result = (Result<?>) invoker.invoke(session, request.getData());
				} else {
					// 获取绑定对象
					Object attachment = session.getAttachment();
					if (attachment != null) {
						Player player = (Player) attachment;
						result = (Result<?>) invoker.invoke(player.getPlayerId(), request.getData());
					} else {
						// 会话未登录拒绝请求
						response.setStateCode(ResultCode.LOGIN_PLEASE);
						session.write(response);
						return;
					}
				}

				// 判断请求是否成功
				if (result.getResultCode() == ResultCode.SUCCESS) {
					// 回写数据
					Object object = result.getContent();
					if (object != null) {
						// 判断用的是那种协议 Serializer 自定义协议 ；GeneratedMessage
						// protobuf协议
						if (object instanceof Serializer) {

							Serializer content = (Serializer) object;
							response.setData(content.getBytes());
						} else if (object instanceof GeneratedMessage) {
							GeneratedMessage content = (GeneratedMessage) object;
							response.setData(content.toByteArray());
						} else {
							System.out.println(String.format("不可识别传输对象:%s", object));
						}
					}
					session.write(response);
				} else {
					// 返回错误码
					response.setStateCode(result.getResultCode());
					session.write(response);
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				// 系统未知异常
				response.setStateCode(ResultCode.UNKOWN_EXCEPTION);
				session.write(response);
			}
		} else {
			// 未找到执行者
			response.setStateCode(ResultCode.NO_INVOKER);
			session.write(response);

		}
		// 当前线程还处于worker线程池的线程中，这个线程原本只是用来处理编解码的，但是后续的业务处理也一块进行了
		// Runnable task = handlerMessage(new SessionImpl(ctx.channel()),
		// request);
		// 把任务放到业务线程池中，整个过程是异步的过程。（要是不用这个线城池，netty处理是同步处理） netty 3
		// executorService.execute(task);
	}
	/**
	 * 消息处理 netty3 中才用这种土方法
	 * 
	 * @param channelId
	 * @param request
	 */
	/*
	 * private Runnable handlerMessage(final Session session, final Request
	 * request) { return new Runnable() {
	 * 
	 * @Override public void run() { log.info("*****当前线程id: "
	 * +Thread.currentThread().getId()); Response response = new
	 * Response(request);
	 * 
	 * log.info("****** module:" + request.getModule() + "   " + "cmd：" +
	 * request.getCmd());
	 * 
	 * // 获取命令执行器 Invoker invoker = InvokerHoler.getInvoker(request.getModule(),
	 * request.getCmd()); if (invoker != null) { try { Result<?> result = null;
	 * // 假如是玩家模块传入channel参数，否则传入playerId参数 if (request.getModule() ==
	 * ModuleId.PLAYER) { result = (Result<?>) invoker.invoke(session,
	 * request.getData()); } else { // 获取绑定对象 Object attachment =
	 * session.getAttachment(); if (attachment != null) { Player player =
	 * (Player) attachment; result = (Result<?>)
	 * invoker.invoke(player.getPlayerId(), request.getData()); } else { //
	 * 会话未登录拒绝请求 response.setStateCode(ResultCode.LOGIN_PLEASE);
	 * session.write(response); return; } }
	 * 
	 * // 判断请求是否成功 if (result.getResultCode() == ResultCode.SUCCESS) { // 回写数据
	 * Object object = result.getContent(); if (object != null) { // 判断用的是那种协议
	 * Serializer 自定义协议 ；GeneratedMessage // protobuf协议 if (object instanceof
	 * Serializer) {
	 * 
	 * Serializer content = (Serializer) object;
	 * response.setData(content.getBytes()); } else if (object instanceof
	 * GeneratedMessage) { GeneratedMessage content = (GeneratedMessage) object;
	 * response.setData(content.toByteArray()); } else {
	 * System.out.println(String.format("不可识别传输对象:%s", object)); } }
	 * session.write(response); } else { // 返回错误码
	 * response.setStateCode(result.getResultCode()); session.write(response);
	 * return; } } catch (Exception e) { e.printStackTrace(); // 系统未知异常
	 * response.setStateCode(ResultCode.UNKOWN_EXCEPTION);
	 * session.write(response); } } else { // 未找到执行者
	 * response.setStateCode(ResultCode.NO_INVOKER); session.write(response);
	 * 
	 * }
	 * 
	 * } };
	 * 
	 * }
	 */

	/**
	 * 断线移除会话
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("***** 会话移除");
		Session session = new SessionImpl(ctx.channel());
		Object object = session.getAttachment();
		if (object != null) {
			Player player = (Player) object;
			SessionManager.removeSession(player.getPlayerId());
		}
	}
}
