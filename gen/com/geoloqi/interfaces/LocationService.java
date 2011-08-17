/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/bpl29/Desktop/Geoloqi-Map-Attack/src/com/geoloqi/interfaces/LocationService.aidl
 */
package com.geoloqi.interfaces;
public interface LocationService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.geoloqi.interfaces.LocationService
{
private static final java.lang.String DESCRIPTOR = "com.geoloqi.interfaces.LocationService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.geoloqi.interfaces.LocationService interface,
 * generating a proxy if needed.
 */
public static com.geoloqi.interfaces.LocationService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.geoloqi.interfaces.LocationService))) {
return ((com.geoloqi.interfaces.LocationService)iin);
}
return new com.geoloqi.interfaces.LocationService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_setPollRate:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
this.setPollRate(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.geoloqi.interfaces.LocationService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void setPollRate(long rate) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(rate);
mRemote.transact(Stub.TRANSACTION_setPollRate, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_setPollRate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void setPollRate(long rate) throws android.os.RemoteException;
}
