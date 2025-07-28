//package com.example.transaction_service.filter;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.dubbo.common.constants.CommonConstants;
//import org.apache.dubbo.common.extension.Activate;
//import org.apache.dubbo.rpc.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//import org.springframework.stereotype.Component;
//
//@Component("dubboProviderSecurityFilter")
//@Activate(group = CommonConstants.PROVIDER)
//public class DubboProviderSecurityFilter implements Filter {
//
//    private static final Logger logger = LoggerFactory.getLogger(DubboProviderSecurityFilter.class);
//
//    @Autowired
//    private ObjectMapper objectMapper;  // Spring Boot mặc định đã cung cấp
//
//    @Override
//    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
//        System.out.println("filterrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr");
//        String secCtxJson = RpcContext.getContext().getAttachment("security_authentication_context");
//
//        if (secCtxJson != null) {
//            try {
//                JwtAuthenticationToken jwtAuth =
//                        objectMapper.readValue(secCtxJson, JwtAuthenticationToken.class);
//                SecurityContextHolder.getContext().setAuthentication(jwtAuth);
//                logger.debug("Restored SecurityContext from RPC context for user {}", jwtAuth.getName());
//            } catch (Exception e) {
//                logger.warn("Failed to parse security_authentication_context: {}", e.getMessage());
//            }
//        } else {
//            logger.debug("No security_authentication_context in RpcContext");
//        }
//
//        try {
//            return invoker.invoke(invocation);
//        } finally {
//            SecurityContextHolder.clearContext();
//        }
//    }
//}
