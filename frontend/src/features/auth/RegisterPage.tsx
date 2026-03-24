import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import { useNotificationStore } from '../../store/notificationStore'

export function RegisterPage() {
  const [email, setEmail] = useState('new-user@khaleo.app')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const register = useAuthStore((state) => state.register)
  const pushSuccess = useNotificationStore((state) => state.pushSuccess)
  const pushError = useNotificationStore((state) => state.pushError)
  const navigate = useNavigate()

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!email || !password || !confirmPassword) {
      setError('Vui lòng nhập đầy đủ email, password và confirm password.')
      return
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu nhập lại không khớp. Vui lòng kiểm tra lại.')
      pushError('Mật khẩu nhập lại không khớp. Vui lòng kiểm tra lại.')
      return
    }

    setError('')
    setMessage('')
    try {
      await register(email, password)
      const normalizedEmail = email.trim().toLowerCase()
      const successMessage = `Đăng ký thành công với email ${normalizedEmail}. Bạn có thể đăng nhập ngay.`
      setMessage(successMessage)
      pushSuccess(successMessage)
      navigate('/login')
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Registration failed'
      setError(errorMessage)
      pushError(errorMessage)
      console.error('register_failed', err)
    }
  }

  return (
    <section className="mx-auto max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-semibold">Register</h1>
      <form className="mt-4 space-y-3" onSubmit={onSubmit}>
        <label className="block">
          <span className="text-sm">Email</span>
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <label className="block">
          <span className="text-sm">Password</span>
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <label className="block">
          <span className="text-sm">Confirm Password</span>
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
          />
        </label>
        <button className="rounded bg-slate-900 px-4 py-2 text-white" type="submit">
          Create account
        </button>
        {error ? <p className="text-sm text-rose-600">{error}</p> : null}
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
      </form>
    </section>
  )
}
