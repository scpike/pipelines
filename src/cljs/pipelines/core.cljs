(ns pipelines.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [pipelines.pipes :as pipes])
    (:use [clojure.string :only (join)])
    (:import goog.History))

;; -------------------------
;; State
(defonce app-state (atom {:text "Hello, this is: "}))

(defonce input (atom "Banana
Fruit
Apple
Zebra
Lemon
Garnish
Fruit Salad
Apple Pie
"))

(defonce pipeline (atom ["echo"]))

(defn get-state [k & [default]]
  (clojure.core/get @app-state k default))

(defn put! [k v]
  (swap! app-state assoc k v))

(defn main-page []
  [:div [(get-state :current-page)]])

(defn add-func-to-pipeline
  [func]
  (swap! pipeline #(conj % func)))

(defn func-input []
  [:div.text-input.box.left
   [:h2 "Function"]
   [:select { :onChange #(add-func-to-pipeline (-> % .-target .-value)) }
    (for [k (keys pipes/func-choices)]
      [:option k])]])

(defn text-input []
  [:div.text-input.box.left
   [:h2 (str "Input")]
   [:textarea {:value @input
               :style { :width "90%" :height "200px"  :overflow-y "scroll" }
               :on-change #(reset! input (-> % .-target .-value))}]])

(defn results []
  (let [in @input
        p @pipeline]
  [:div
   [:h2 "Pipeline"]
   (print p)
   (print (partition 2 (interleave p (pipes/compute-pipeline p in))))
   (for [piece (partition 2 (interleave p (pipes/compute-pipeline p in)))]
     [:div
      [:h3 (str (first piece))]
      [:textarea {:value (last piece)
                  :readOnly true
                  :style { :width "90%" :height "200px" :overflow-y "scroll" }}]])]))

(defn new-step []
  [:div
   [func-input]])

(defn page1 []
  [:div
   [text-input]
   [results]
   [new-step]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (put! :current-page page1))

;; -------------------------
;; Initialize app
(defn init! []
  (reagent/render-component [main-page] (.getElementById js/document "app")))

;; -------------------------
;; History
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))
;; need to run this after routes have been defined
(hook-browser-navigation!)
