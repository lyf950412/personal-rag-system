import { useState, useCallback, useRef, useEffect } from 'react'

export function useDebounce(callback, delay) {
  const timeoutRef = useRef(null)
  const callbackRef = useRef(callback)

  useEffect(() => {
    callbackRef.current = callback
  }, [callback])

  const debouncedCallback = useCallback((...args) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    timeoutRef.current = setTimeout(() => {
      callbackRef.current(...args)
    }, delay)
  }, [delay])

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  return debouncedCallback
}

export function useThrottle(callback, delay) {
  const lastRun = useRef(Date.now())

  return useCallback((...args) => {
    const now = Date.now()
    if (now - lastRun.current >= delay) {
      callback(...args)
      lastRun.current = now
    }
  }, [callback, delay])
}

export function usePrevious(value) {
  const ref = useRef()
  
  useEffect(() => {
    ref.current = value
  }, [value])
  
  return ref.current
}

export function useLocalStorage(key, initialValue) {
  const [storedValue, setStoredValue] = useState(() => {
    try {
      const item = window.sessionStorage.getItem(key)
      return item ? JSON.parse(item) : initialValue
    } catch (error) {
      console.error('Error reading from localStorage:', error)
      return initialValue
    }
  })

  const setValue = (value) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value
      setStoredValue(valueToStore)
      window.sessionStorage.setItem(key, JSON.stringify(valueToStore))
    } catch (error) {
      console.error('Error writing to localStorage:', error)
    }
  }

  return [storedValue, setValue]
}
