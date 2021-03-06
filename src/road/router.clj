(ns road.router
  (:require [clojure.tools.macro :as macro]
            [clout.core :as clout]
            [clojure.tools.logging :as log]))

(defn- convert [val type]
  (cond (= "Integer" type) (Integer/parseInt val)
        (= "Float" type) (Float/parseFloat val)
        (= "Double" type) (Double/parseDouble val)
        :else val))

(defn- get-tag [arg]
  (-> arg meta :tag str))

(defn- convert-type-to-string [args]
  (-> (map str args) vec))

(defn- get-request-para [name req paras]
  (or (get (:params req) name) ((keyword name) paras) 
      (throw (Exception. (str "No such parameter exception: " name)))))

(defn- get-para [arg req paras]
  (if (= "req" (str arg)) req 
    (-> arg str (get-request-para req paras) (convert (get-tag arg)))))

(defn- get-all-paras [req paras args]
  (map #(get-para % req paras) args))

(defn parse-arguments [f req paras]
  (-> (meta f) :arglists first (#(get-all-paras req paras %))  vec))

(defn- prepare-route [route]
  (clout/route-compile route))

(defn- if-route [route handler]
  (fn [request]
    (log/debug "HTTP URI: " (:uri request))
    (if-let [ret (clout/route-matches route request)] 
      (handler request ret)))) 

(defn- if-method [method handler]
  (fn [request]
  (println (:request-method  request))
    (if (= method (:request-method  request)) 
           (handler request))))

(defn- process-request [handler request paras]
  (log/debug "Road-handler: " handler)
  (log/debug "Ring-request: " request)
  (apply handler (parse-arguments handler request paras)))

(defn make-route  [method path handler]
  (if-method method
             (if-route path
                       (fn [request paras]
                         (process-request handler request paras)))))

(defn compile-route [method path handler]
  `(make-route
     ~method ~(prepare-route path) (var ~handler)))

(defn routing [request & handlers]
  (some #(% request) handlers))

(defn routes [& handlers] 
  #(apply routing % handlers))

(defmacro defroutes [name & routes]
  (let [[name routes] (macro/name-with-attributes name routes)] 
    `(def ~name (routes ~@routes))))

