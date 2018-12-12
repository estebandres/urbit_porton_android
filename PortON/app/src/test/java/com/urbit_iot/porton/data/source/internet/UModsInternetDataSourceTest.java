package com.urbit_iot.porton.data.source.internet;

import androidx.annotation.NonNull;


import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.DeleteUserRPC;
import com.urbit_iot.porton.data.rpc.FactoryResetRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.GetUsersRPC;
import com.urbit_iot.porton.data.rpc.RPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.rpc.UpdateUserRPC;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class UModsInternetDataSourceTest {

    @NonNull
    @Mock
    private FirmwareFileDownloader mFirmwareFileDownloaderMock;
    @NonNull
    @Mock
    private UModMqttServiceContract mUModMqttServiceMock;
    @NonNull
    private String username;

    private UModsInternetDataSource uModsInternetDataSource;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Before
    public void setUp() throws Exception {
        uModsInternetDataSource= new UModsInternetDataSource(
                mFirmwareFileDownloaderMock,
                mUModMqttServiceMock,
                username
        );
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(mFirmwareFileDownloaderMock,mUModMqttServiceMock);
        reset(
                mFirmwareFileDownloaderMock,
                mUModMqttServiceMock
        );
        uModsInternetDataSource=null;
    }

    @Test
    public void When_getUModsOneByOne_Then_ServicioMqttBuscaInvitaciones(){
        //WHEN
        uModsInternetDataSource.getUModsOneByOne();
        //THEN
        verify(mUModMqttServiceMock,times(1)).scanUModInvitations();
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void Given_ModuloPertenecienteUser_When_saveUMod_Then_ServicioMqttLlamaFuncionSubscribeToUModResponseTopic(){
        //GIVEN
        UMod uModMock = new UMod("123");
        uModMock.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        //WHEN
        uModsInternetDataSource.saveUMod(uModMock);
        //THEN
        verify(mUModMqttServiceMock,times(1)).subscribeToUModTopics(uModMock);
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_deleteAllUMods_Then_ServicioMqttBorraTodasSuscripciones(){
        //WHEN
        uModsInternetDataSource.deleteAllUMods();
        //THEN
        verify(mUModMqttServiceMock,times(1)).clearAllSubscriptions();
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_getUserLevel_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        GetUserLevelRPC.Arguments arguments= new GetUserLevelRPC.Arguments("TestGetUserLevel");
        GetUserLevelRPC.Result result= new GetUserLevelRPC.Result("test",APIUserType.User);
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        GetUserLevelRPC.Response response= new GetUserLevelRPC.Response(result,0,"test",error);
        when(mUModMqttServiceMock.publishRPC( any(UMod.class) ,any(GetUserLevelRPC.Request.class), any()))
                .thenReturn(rx.Observable.just(response));

        //WHEN
        uModsInternetDataSource.getUserLevel(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_triggerUMod_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        TriggerRPC.Arguments arguments= new TriggerRPC.Arguments();
        TriggerRPC.Result result= new TriggerRPC.Result("Resultado");
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        TriggerRPC.Response response= new TriggerRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(TriggerRPC.Request.class), any(), any(Integer.class)
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.triggerUMod(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_createUModUser_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        CreateUserRPC.Arguments arguments= new CreateUserRPC.Arguments("testCreateUModUser");
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        CreateUserRPC.Result result= new CreateUserRPC.Result(APIUserType.User);
        CreateUserRPC.Response response= new CreateUserRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(CreateUserRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.createUModUser(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_updateUModUser_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        UpdateUserRPC.Arguments arguments= new UpdateUserRPC.Arguments("testUpdateUModUser", APIUserType.User);
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        UpdateUserRPC.Result result= new UpdateUserRPC.Result("test");
        UpdateUserRPC.Response response= new UpdateUserRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(UpdateUserRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.updateUModUser(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_deleteUModUser_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        DeleteUserRPC.Arguments arguments= new DeleteUserRPC.Arguments("testDeleteUModUser");
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        DeleteUserRPC.Result result= new DeleteUserRPC.Result("test");
        DeleteUserRPC.Response response= new DeleteUserRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(DeleteUserRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.deleteUModUser(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_getUModUsers_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        GetUsersRPC.Arguments arguments= new GetUsersRPC.Arguments();
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        GetUsersRPC.UserResult userResult= new GetUsersRPC.UserResult("UserList", APIUserType.User);
        GetUsersRPC.Result result= new GetUsersRPC.Result(Arrays.asList(userResult));
        GetUsersRPC.Response response= new GetUsersRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(GetUsersRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.getUModUsers(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_getSystemInfo_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        SysGetInfoRPC.Arguments arguments= new SysGetInfoRPC.Arguments();
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        SysGetInfoRPC.Result result= new SysGetInfoRPC.Result("","","","",""
                ,0,0,0,0,0
                ,0,new SysGetInfoRPC.Result.Wifi(null,null,null,null),null);
        SysGetInfoRPC.Response response= new SysGetInfoRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(SysGetInfoRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.getSystemInfo(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_factoryResetUMod_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        FactoryResetRPC.Arguments arguments= new FactoryResetRPC.Arguments();
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        FactoryResetRPC.Result result= new FactoryResetRPC.Result("Test");
        FactoryResetRPC.Response response= new FactoryResetRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(FactoryResetRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.factoryResetUMod(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

    @Test
    public void When_getFirmwareImageFile_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        //WHEN
        uModsInternetDataSource.getFirmwareImageFile(uModMock);
        //THEN
        verify(mFirmwareFileDownloaderMock,times(1)).downloadFirmwareFile();
        verifyZeroInteractions(mUModMqttServiceMock);
    }

    @Test
    public void When_createUModUserByName_Then_ServicioMqttPublicaRPC(){
        //GIVEN
        UMod uModMock = new UMod("123");
        AdminCreateUserRPC.Arguments arguments= new AdminCreateUserRPC.Arguments("testCreateUModUserByName");
        RPC.ResponseError error= new RPC.ResponseError(1,"error");
        AdminCreateUserRPC.Result result= new AdminCreateUserRPC.Result(APIUserType.Admin);
        AdminCreateUserRPC.Response response= new AdminCreateUserRPC.Response(result,0,null,error);
        when(mUModMqttServiceMock.publishRPC(
                any(UMod.class) ,any(AdminCreateUserRPC.Request.class), any()
        )).thenReturn(rx.Observable.just(response));
        //WHEN
        uModsInternetDataSource.createUModUserByName(uModMock,arguments).test()
                .assertValue(response.getResponseResult());
        //THEN
        verifyZeroInteractions(mFirmwareFileDownloaderMock);
    }

}