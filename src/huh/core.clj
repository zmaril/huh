(ns huh.core
  (:require [instaparse.core :as insta]
            [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as j]
            [clojure.string :as s]
            [clojure.data.json :as json]
            [me.raynes.fs    :refer [glob]]))

(def example
  "2511  1466310826.177350 [00007fb71626959e] rt_sigaction(\"\\n\") = 0 <0.000003>"
  )

(def strace-grammar
  "line = ((normal | exit) <'\n'>?)+ 
   normal        = pid <(' '|'  ')> time <' '> instruction <' '> call <'('> call_args <')'> <' = '> ((return <' '> total) | question)
   question    = '?'
   exit        = pid <(' '|'  ')> time <' '> '[????????????????] +++ exited with 0 +++' 
   pid         = #'\\d+'
   time        = #'\\d+' '.' #'\\d+'
   instruction = <'['> #'[0-9a-f]+' <']'> 
   call        = #'[a-z_]+' 
   call_args   = args
   args        = arg (<', '> arg )*
   <arg>       = string | null | sigs | number | array | hex | file | struct | timestamp | weird_arr | weirder_arr | net  
   timestamp   = #'\\d{4}/\\d{2}/\\d{2}-\\d{2}:\\d{2}:\\d{2}(.\\d{9})?'
   struct      = <'{'> member (<', '> member)* <'}'>
   weird_arr   = <'{'> args <'}'>
   weirder_arr   = '~'? <'['> sig <' '> sig <']'>
   member      = #'[a-z_]+'(<'('> number <')'>)? <'='> (arg | fncall | expr)
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
   return      = arg (return_msg | return_arg | return_flags)?
   return_arg  = (<' ('> arg <')'>)
   return_msg  = <' '> #'[A-Z_]+' <' ('> #'[ A-Za-z]+' <')'>
   return_flags = <' (flags '> sigs <')'>
   total       = <'<'> #'[0-9]+' '.' #'[0-9]+' <'>'>" )

(def parser
  (insta/parser strace-grammar))

(defn parse-strace [lines]
  (->> lines s/split-lines (map parser) (cons :lines) vec))

(defn stringer [& args]
  (str "'" (apply str args) "'")
  )

(defn jsoner [arg] (stringer (json/write-str arg)))

(defn strace->insert [table-name]
  {:lines (fn [& args] (s/join ";\n" args ))
   :line identity
   :normal  (fn [& args]
            (str "insert into " table-name
                 " values ("
                 (s/join "," args)
                 ")"))
   :pid identity
   :instruction stringer
   :args (fn [& args] args)
   :call stringer
   :time (fn [& args] (str "to_timestamp(" (s/join "" args) ")"))
   :call_args (fn [args] (jsoner args))
   :return (fn [v & args]
             (if (empty? args)
               (str (jsoner v) ", null")
               (str (jsoner v) "," (s/join "," args))))
   :array (fn [arg] arg)
   :string (fn [& args] (apply str args))
   :hex    (fn [& args] (apply str args))
   :struct (fn [& kvs] (str "{" (s/join "," kvs) "}"))
   :member (fn [k v] (str "\"" k "\":" v))
   :return_arg jsoner
   :return_msg stringer
   :return_flags jsoner
   :number identity
   :total (fn [& args] (str (apply str args) " * interval '1 second'"))
   :question (fn [& args] "null")
   :exit (fn [pid time & args]
           (str "insert into " table-name " values ("
                pid "," time ",null,'exited',null,null,null,null)"))
   })
;;Someplace args is getting json wrapped twice

(defn -main [strace-file table-name insert-file]
  (let [result (parse-strace (slurp strace-file))]
    (if (insta/failure? result)
      (do 
        (print result)
        (System/exit -1))
      (spit insert-file
            (insta/transform (strace->insert table-name) result)))))

