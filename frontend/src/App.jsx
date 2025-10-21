import React, { useEffect, useState } from 'react'
import { createClient } from '@supabase/supabase-js'

// Replace with your Supabase URL and anon key in environment or here for testing
const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || ''
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || ''

const supabase = (SUPABASE_URL && SUPABASE_ANON_KEY)
  ? createClient(SUPABASE_URL, SUPABASE_ANON_KEY)
  : null

export default function App() {
  const [message, setMessage] = useState('Welcome to Cramer - IELTS')

  useEffect(() => {
    if (!supabase) return

    // example: fetch public table called 'posts'
    ;(async () => {
      const { data, error } = await supabase.from('posts').select('*').limit(1)
      if (error) {
        console.log('Supabase error', error)
        return
      }
      if (data && data.length) setMessage(`Loaded ${data.length} post(s) from Supabase`)
    })()
  }, [])

  return (
    <div className="container">
      <h1>{message}</h1>
      <p>This is the starter frontend for Cramer (React + Vite)</p>
    </div>
  )
}
