import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { User, AuthResponse, LoginRequest, RegisterRequest, UserRole } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly API = `${environment.apiUrl}/auth`;

  /** Estado reactivo del usuario actual */
  currentUser = signal<User | null>(null);

  /** Computed signals derivadas del estado del usuario */
  isAuthenticated = computed(() => !!this.currentUser());
  isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  userRole = computed<UserRole | 'GUEST'>(() => this.currentUser()?.role ?? 'GUEST');
  displayName = computed(() => this.currentUser()?.fullName ?? '');

  constructor() {
    this.loadUserFromStorage();
  }

  /**
   * Iniciar sesión con email y contraseña.
   * Almacena el JWT en localStorage y actualiza el estado del usuario.
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/login`, credentials).pipe(
      tap(response => {
        this.storeSession(response);
      })
    );
  }

  /**
   * Registrar un nuevo usuario.
   * Almacena el JWT en localStorage y actualiza el estado del usuario.
   */
  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/register`, data).pipe(
      tap(response => {
        this.storeSession(response);
      })
    );
  }

  /**
   * Iniciar sesión con Google (recibe el idToken).
   */
  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API}/google`, { idToken }).pipe(
      tap(response => {
        this.storeSession(response);
      })
    );
  }

  /**
   * Obtener el perfil del usuario actual desde el backend.
   */
  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.API}/me`).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  /**
   * Cerrar sesión: elimina token, limpia estado y redirige al home.
   */
  logout(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_data');
    this.currentUser.set(null);
    this.router.navigate(['/']);
  }

  /**
   * Obtener el token JWT almacenado.
   */
  getToken(): string | null {
    return localStorage.getItem('jwt_token');
  }

  // --- Métodos privados ---

  private storeSession(response: AuthResponse): void {
    localStorage.setItem('jwt_token', response.token);
    localStorage.setItem('user_data', JSON.stringify(response.user));
    this.currentUser.set(response.user);
  }

  private loadUserFromStorage(): void {
    const userData = localStorage.getItem('user_data');
    if (userData) {
      try {
        this.currentUser.set(JSON.parse(userData));
      } catch {
        this.logout();
      }
    }
  }
}
