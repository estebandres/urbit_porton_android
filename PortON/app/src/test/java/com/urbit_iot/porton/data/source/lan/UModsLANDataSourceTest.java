package com.urbit_iot.porton.data.source.lan;


import com.urbit_iot.porton.data.UMod;
import com.urbit_iot.porton.data.UModUser;
import com.urbit_iot.porton.data.rpc.APIUserType;
import com.urbit_iot.porton.data.rpc.AdminCreateUserRPC;
import com.urbit_iot.porton.data.rpc.CreateUserRPC;
import com.urbit_iot.porton.data.rpc.DeleteUserRPC;
import com.urbit_iot.porton.data.rpc.FactoryResetRPC;
import com.urbit_iot.porton.data.rpc.GetUserLevelRPC;
import com.urbit_iot.porton.data.rpc.GetUsersRPC;
import com.urbit_iot.porton.data.rpc.OTACommitRPC;
import com.urbit_iot.porton.data.rpc.SetGateStatusRPC;
import com.urbit_iot.porton.data.rpc.SetWiFiRPC;
import com.urbit_iot.porton.data.rpc.SysGetInfoRPC;
import com.urbit_iot.porton.data.rpc.TriggerRPC;
import com.urbit_iot.porton.data.rpc.UpdateUserRPC;
import com.urbit_iot.porton.util.networking.UrlHostSelectionInterceptor;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UModsLANDataSourceTest {

    @Mock
    private UModsDNSSDScanner mUModsDNSSDScannerMock;

    @Mock
    private UModsBLEScanner mUModsBLEScannerMock;

    @Mock
    private UModsWiFiScanner mUModsWiFiScannerMock;

    @Mock
    private UModsTCPScanner mUModsTCPScannerMock;

    @Mock
    private UModsService defaultUModsServiceMock;

    @Mock
    private UModsService appUserUModsServiceMock;

    @Mock
    private UrlHostSelectionInterceptor urlHostSelectionInterceptorMock;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    private UModsLANDataSource uModsLANDataSource;

    @Before
    public void setUp() throws Exception {
        uModsLANDataSource = new UModsLANDataSource(
                mUModsDNSSDScannerMock,
                mUModsBLEScannerMock,
                mUModsWiFiScannerMock,
                urlHostSelectionInterceptorMock,
                defaultUModsServiceMock,
                appUserUModsServiceMock,
                mUModsTCPScannerMock);
    }

    @After
    public void tearDown() throws Exception {
        reset( mUModsDNSSDScannerMock,
                mUModsBLEScannerMock,
                mUModsWiFiScannerMock,
                mUModsTCPScannerMock,
                defaultUModsServiceMock,
                appUserUModsServiceMock,
                urlHostSelectionInterceptorMock);
        uModsLANDataSource = null;
    }

    @Test
    public void When_getUMod_Then_buscaModulosPorLanAndWiFiyMarcaModulosComoLanyDevuelvePrimero(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        when(mUModsWiFiScannerMock.browseWiFiForUMod(moduloMock.getUUID())).thenReturn(Observable.just(moduloMock));
        when(mUModsDNSSDScannerMock.browseLANForUMod(moduloMock.getUUID())).thenReturn(Observable.just(moduloMock));
        //WHEN
        uModsLANDataSource.getUMod(moduloMock.getUUID()).test()
                .assertCompleted()
                .assertValueCount(1);
        //THEN
        verify(mUModsDNSSDScannerMock,times(1)).browseLANForUMod(moduloMock.getUUID());
        verify(mUModsWiFiScannerMock,times(1)).browseWiFiForUMod(moduloMock.getUUID());
        assertEquals(UMod.UModSource.LAN_SCAN,moduloMock.getuModSource());
    }

    @Test
    public void When_getUserLevel_Then_seteoDireccionConexionYDevuelvoNivelUsario(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        GetUserLevelRPC.Arguments arguments = new GetUserLevelRPC.Arguments("userLevel");
        //WHEN
        uModsLANDataSource.getUserLevel(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(defaultUModsServiceMock,times(1)).getAppUserLevel(arguments);
    }

    @Test
    public void Given_siendoAdministrador_When_triggerUMod_Then_seteoDireccionConexionYDevuelvoTriggerAdmin(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        moduloMock.setAppUserLevel(UModUser.Level.ADMINISTRATOR);
        TriggerRPC.Arguments arguments = new TriggerRPC.Arguments();
        //WHEN
        uModsLANDataSource.triggerUMod(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).adminTriggerUMod(arguments);
    }

    @Test
    public void Given_noSiendoAdministrado_When_triggerUMod_Then_seteoDireccionConexionYDevuelvoTriggerUser(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        moduloMock.setAppUserLevel(UModUser.Level.AUTHORIZED);  // PROBE CON DESAUTORIZADO Y AUTORIZADO Y LAS DOS ENTRAN AL ELSE         // NO HAY QUE ACLARLO EN EL CÓDIGO
        TriggerRPC.Arguments arguments = new TriggerRPC.Arguments();
        //WHEN
        uModsLANDataSource.triggerUMod(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).userTriggerUMod(arguments);
    }

    @Test
    public void When_createUModUser_Then_seteoDireccionConexionYDevuelvoSolicitudDeCreacionUsuario(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        CreateUserRPC.Arguments arguments = new CreateUserRPC.Arguments("arguments");
        //WHEN
        uModsLANDataSource.createUModUser(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(defaultUModsServiceMock,times(1)).createUser(arguments);

    }

    @Test
    public void When_updateUModUser_Then_seteoDireccionConexionYDevuelvoSolicitudActualizacionUsuario(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        UpdateUserRPC.Arguments arguments = new UpdateUserRPC.Arguments("arguments", APIUserType.User);
        //WHEN
        uModsLANDataSource.updateUModUser(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).postUpdateUser(arguments);

    }

    @Test
    public void When_deleteUModUser_Then_seteoDireccionConexionYDevuelvoSolicitudBorrarUsuario(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        DeleteUserRPC.Arguments arguments = new DeleteUserRPC.Arguments("arguments");
        //WHEN
        uModsLANDataSource.deleteUModUser(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).deleteUser(arguments);
    }

    @Test
    public void When_getUModUsers_Then_seteoDireccionConexionYDevuelvoSolicitudObtenerUsuarios(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        GetUsersRPC.Arguments arguments = new GetUsersRPC.Arguments();
        //WHEN
        uModsLANDataSource.getUModUsers(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).getUsers(arguments);
    }

    @Test
    public void When_getSystemInfo_Then_seteoDireccionConexionYDevuelvoSolicitudDeObtenerInformacion(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        SysGetInfoRPC.Arguments arguments = new SysGetInfoRPC.Arguments();
        //WHEN
        uModsLANDataSource.getSystemInfo(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(defaultUModsServiceMock,times(1)).getSystemInfo(arguments);
    }

    @Test
    public void When_setWiFiAP_Then_seteoDireccionConexionYDevuelvoSolicitudCredencialesWiFi(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        SetWiFiRPC.Arguments arguments = new SetWiFiRPC.Arguments("argumentos","args");
        //WHEN
        uModsLANDataSource.setWiFiAP(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).postWiFiAPCredentials(arguments);
    }

    @Test
    public void When_postFirmwareUpdateToUMod_Then_seteoDireccionConexionYDevuelvoComienzoActualizacionModulo(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        File file= new File("mock");
        //WHEN
        uModsLANDataSource.postFirmwareUpdateToUMod(moduloMock, file);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).startFirmwareUpdate(any(RequestBody.class),any(MultipartBody.Part.class));
    }

    @Test
    public void When_otaCommit_Then_seteoDireccionConexionYDevuelvoSolicitudConfiramcionActualizacion(){
        //Given
        UMod moduloMock= new UMod("111");
        OTACommitRPC.Arguments arguments = new OTACommitRPC.Arguments();
        //WHEN
        uModsLANDataSource.otaCommit(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(defaultUModsServiceMock,times(1)).otaCommit(arguments);
    }

    @Test
    public void When_factoryResetUMod_Then_seteoDireccionConexionYDevuelvoSolicitudFactoryReset(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        FactoryResetRPC.Arguments arguments = new FactoryResetRPC.Arguments();
        //WHEN
        uModsLANDataSource.factoryResetUMod(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).postFactoryReset(arguments);
    }

    @Test
    public void When_createUModUserByName_Then_seteoDireccionConexionYDevuelvoSolicitudCreacionUserAdmin(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        AdminCreateUserRPC.Arguments arguments = new AdminCreateUserRPC.Arguments("argumentos");
        //WHEN
        uModsLANDataSource.createUModUserByName(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).postAdminCreateUser(arguments);
    }

    @Test
    public void When_setUModGateStatus_Then_seteoDireccionConexionYDevuelvoSolicitudSeteoEstadoModulo(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        SetGateStatusRPC.Arguments arguments = new SetGateStatusRPC.Arguments(UMod.GateStatus.OPEN.getStatusID());
        //WHEN
        uModsLANDataSource.setUModGateStatus(moduloMock,arguments);
        //THEN
        verify(urlHostSelectionInterceptorMock,times(1)).setHost(moduloMock.getConnectionAddress());
        verify(appUserUModsServiceMock,times(1)).postSetUModGateStatus(arguments);
    }

    @Test
    public void When_getUMods_Then_buscaModulosPorLanYBLEYDevuelveListaConModulosMarcados(){
        //GIVEN
        UMod moduloMock= new UMod("111");
        when(mUModsDNSSDScannerMock.browseLANForUMods()).thenReturn(Observable.just(moduloMock));
        when(mUModsBLEScannerMock.bleScanForUMods()).thenReturn(Observable.empty());
        //WHEN
        RxJavaInterop.toV2Flowable(uModsLANDataSource.getUMods())
                .test()
                .assertValue(uMods -> uMods.size() == 1
                        && uMods.contains(moduloMock))
                .assertComplete();
        //THEN
        verify(mUModsDNSSDScannerMock,times(1)).browseLANForUMods();
        verify(mUModsBLEScannerMock,times(1)).bleScanForUMods();
        verifyNoMoreInteractions(mUModsDNSSDScannerMock);
        verifyNoMoreInteractions(mUModsBLEScannerMock);
        assertEquals(UMod.UModSource.LAN_SCAN,moduloMock.getuModSource());
    }

    @Test
    public void When_getUModsOneByOne_Then_buscaModulosPorLanYWiFiYLosDevuelveMarcadosPorLan(){
        //GIVEN
        UMod moduloMockLan= new UMod("111");
        UMod moduloMockWiFi= new UMod("222");
        when(mUModsWiFiScannerMock.browseWiFiForUMods()).thenReturn(Observable.just(moduloMockWiFi));
        when(mUModsDNSSDScannerMock.browseLANForUMods()).thenReturn(Observable.just(moduloMockLan));
        //WHEN
        uModsLANDataSource.getUModsOneByOne().test();
        //THEN
        verify(mUModsTCPScannerMock,times(1)).setupCalculator();
        verify(mUModsDNSSDScannerMock,times(1)).browseLANForUMods();
        verify(mUModsWiFiScannerMock,times(1)).browseWiFiForUMods();
        verifyNoMoreInteractions(mUModsDNSSDScannerMock);
        verifyNoMoreInteractions(mUModsTCPScannerMock);
        verifyNoMoreInteractions(mUModsWiFiScannerMock);
        assertEquals(UMod.UModSource.LAN_SCAN,moduloMockLan.getuModSource());
        assertEquals(UMod.UModSource.LAN_SCAN,moduloMockWiFi.getuModSource());
    }
}