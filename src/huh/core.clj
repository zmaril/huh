(ns huh.core
  (:require [instaparse.core :as insta]
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs    :refer [glob]]))
(def example
  "20319 1465791874.495946 [00007f29dea8fe70] poll([{fd=4<UDP:[10.0.2.15:48137->10.0.2.3:53]>, events=POLLOUT}], 1, 0) = 1 ([{fd=4, revents=POLLOUT}]) <0.000006>"
  )

(def short
  "[{fd=4<UDP:[10.0.2.15:48137->10.0.2.3:53]>, events=POLLOUT}]"
  )

(def strace-grammar
  "lines = ((line | exit) <'\n'>?)+ 
   line        = pid <' '> time <' '> instruction <' '> command <'('> args <')'> <' = '> ((return <' '> total) | '?')
   exit        = pid <' '> time <' '> '[????????????????] +++ exited with 0 +++' 
   pid         = #'[0-9]+'
   time        = #'[0-9]+' '.' #'[0-9]+'
   instruction = <'['> #'[0-9a-f]+' <']'> 
   command     = #'[a-z_]+' 
   args        = arg (<', '> arg )*
   <arg>       = string | null | sigs | number | array | hex | file | struct | timestamp | weird_arr | weirder_arr | net  
   timestamp   = #'\\d{4}/\\d{2}/\\d{2}-\\d{2}:\\d{2}:\\d{2}(.\\d{9})?'
   struct      = <'{'> member (<', '> member)* <'}'>
   weird_arr   = <'{'> args <'}'>
   weirder_arr   = <'['> sig <' '> sig <']'>
   member      = #'[a-z_]+' <'='> (arg | fncall | expr)
   net         = #'\\d' <'<'> ('TCP' | 'UDP') <':['> ip <'->'> ip <']>'>
   ip          = #'\\d+.\\d+.\\d+.\\d+' <':'>  #'\\d+'
   expr        = arg '*' arg
   fncall      = #'[a-z_]+' '(' args ')'
   file        = number <'<'> #'[^>]+' <'>'>
   null        = 'NULL'
   hex         = '0x' #'[0-9a-f]*'
   array       = <'['> args <']'>
   string      = <'\"'> #'(\\\\.|[^\"])*' <'\"'> ('...')?
   number      = #'[-+]?[0-9]*\\.?[0-9]*'   
   sigs        = sig (<'|'> sig)*
   <sig>       = #'[0-9A-Z_]+' | #'[0-9]+'
   return      = arg (return_msg | (<' ('> arg <')'>))? 
   return_msg  = <' '> #'[A-Z_]+' <' ('> #'[ A-Za-z]+' <')'>
   total       = <'<'> #'[0-9]+' '.' #'[0-9]+' <'>'>" )

(def parse-strace (insta/parser strace-grammar))

(def examples (map #(parse-strace %) (clojure.string/split-lines example)))

(defn -main [& args]
  (let [result (parse-strace (slurp (first args)))]
    (if (insta/failure? result)
      (print result)
      (pprint result)
      )
    )
  )
