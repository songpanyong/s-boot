package com.guohuai;

import org.jasypt.intf.service.JasyptStatelessService;


public final class JasyptPBEStringEncryption {
    /**
     * <p>
     * CLI execution method.
     * </p>
     *
     * @param args the command execution arguments
     */
	public static void main(final String[] args) {

		try {
			final JasyptStatelessService service = new JasyptStatelessService();
			// 需要加密的内容
			final String input = "guohuaiGUO4056&";//
			final String password = "e9fbdb2d3b213c28575c095ae0029e05f40f77ee53ecd24af815bdff5479dd2a"; //
			final String algorithm = "PBEWithMD5AndDES"; //

			final String result = service.encrypt(input, password, null, null, algorithm, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null);
			System.out.println("input=" + input);
			System.out.println("result=" + result);
		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

}
