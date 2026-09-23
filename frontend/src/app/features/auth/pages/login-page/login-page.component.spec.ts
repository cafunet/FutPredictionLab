import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LoginPageComponent } from './login-page.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../../../../core/services/auth.service';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';

describe('LoginPageComponent', () => {
  let component: LoginPageComponent;
  let fixture: ComponentFixture<LoginPageComponent>;
  let authServiceSpy: any;
  let router: Router;

  beforeEach(async () => {
    // Creamos mocks con Vitest
    authServiceSpy = {
      login: vi.fn(),
      loginWithGoogle: vi.fn(),
      isAdmin: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        LoginPageComponent,
        HttpClientTestingModule,
        RouterTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    // Simulamos la variable global de Google
    (window as any).google = {
      accounts: {
        id: {
          initialize: vi.fn(),
          renderButton: vi.fn()
        }
      }
    };

    fixture = TestBed.createComponent(LoginPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword()).toBe(false);
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBe(true);
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBe(false);
  });

  describe('doLogin()', () => {
    it('should show error if credentials are missing', () => {
      component.doLogin('', '123456');
      expect(component.loginError()).toBe('Faltan credenciales.');

      component.doLogin('test@test.com', '');
      expect(component.loginError()).toBe('Faltan credenciales.');
    });

    it('should navigate to / if normal user logs in successfully', () => {
      authServiceSpy.login.mockReturnValue(of({}));
      authServiceSpy.isAdmin.mockReturnValue(false);

      component.doLogin('user@test.com', '123456');

      expect(authServiceSpy.login).toHaveBeenCalledWith({ email: 'user@test.com', password: '123456' });
      expect(router.navigate).toHaveBeenCalledWith(['/']);
      expect(component.loginError()).toBeNull();
    });

    it('should navigate to /admin if admin logs in successfully', () => {
      authServiceSpy.login.mockReturnValue(of({}));
      authServiceSpy.isAdmin.mockReturnValue(true);

      component.doLogin('admin@test.com', 'admin123');

      expect(router.navigate).toHaveBeenCalledWith(['/admin']);
    });

    it('should set loginError if login fails with server message', () => {
      const mockError = { error: { message: 'Contraseña incorrecta' } };
      authServiceSpy.login.mockReturnValue(throwError(() => mockError));

      component.doLogin('user@test.com', 'wrongpassword');

      expect(component.loginError()).toBe('Contraseña incorrecta');
    });

    it('should set default loginError if login fails without server message', () => {
      const mockError = { error: {} };
      authServiceSpy.login.mockReturnValue(throwError(() => mockError));

      component.doLogin('user@test.com', 'wrongpassword');

      expect(component.loginError()).toBe('Credenciales inválidas. Intenta de nuevo.');
    });
  });

  describe('handleGoogleCredentialResponse()', () => {
    it('should do nothing if response has no credential', () => {
      component.handleGoogleCredentialResponse({});
      expect(authServiceSpy.loginWithGoogle).not.toHaveBeenCalled();
    });

    it('should navigate to / if normal user logs in successfully with Google', () => {
      authServiceSpy.loginWithGoogle.mockReturnValue(of({}));
      authServiceSpy.isAdmin.mockReturnValue(false);

      component.handleGoogleCredentialResponse({ credential: 'fake-jwt-token' });

      expect(authServiceSpy.loginWithGoogle).toHaveBeenCalledWith('fake-jwt-token');
      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should navigate to /admin if admin logs in successfully with Google', () => {
      authServiceSpy.loginWithGoogle.mockReturnValue(of({}));
      authServiceSpy.isAdmin.mockReturnValue(true);

      component.handleGoogleCredentialResponse({ credential: 'fake-jwt-admin-token' });

      expect(router.navigate).toHaveBeenCalledWith(['/admin']);
    });

    it('should set loginError if Google login fails', () => {
      const mockError = { error: { message: 'Google Auth Failed' } };
      authServiceSpy.loginWithGoogle.mockReturnValue(throwError(() => mockError));

      component.handleGoogleCredentialResponse({ credential: 'fake-token' });

      expect(component.loginError()).toBe('Google Auth Failed');
    });
  });
});
