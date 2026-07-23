package com.ufs.core.common.error;

/**
 * 厂商（OSS/Kodo/TOS）异常。在 adapter 层捕获并封装，
 * 由 GlobalExceptionHandler 映射为 UFS.5020（错误）或 UFS.5040（超时/不可用），
 * 并透传 vendorCode / vendorMessage 供排查（调用方不据此分支）。
 */
public class VendorException extends RuntimeException {

    private final String vendor;
    private final String vendorCode;
    private final boolean timeout;

    public VendorException(String vendor, String vendorCode, String vendorMessage, boolean timeout) {
        super("vendor=" + vendor + " code=" + vendorCode + " msg=" + vendorMessage);
        this.vendor = vendor;
        this.vendorCode = vendorCode;
        this.timeout = timeout;
    }

    public ErrorCode toUfsErrorCode() {
        return timeout ? UfsErrorCode.VENDOR_TIMEOUT : UfsErrorCode.VENDOR_ERROR;
    }

    public String getVendor() {
        return vendor;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
