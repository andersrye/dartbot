(ns dartbot.statemachine)

(defn find-first  [pred coll]
  (first (filter pred coll)))

(defn state-machine [transition-table initial-state]
  (ref initial-state :meta transition-table))

(defn- switch-state? [conds]
  (if (empty? conds)
    true
    (not (some false? (reduce #(conj %1 (if (fn? %2) (%2) %2)) [] conds)))))

(defn- first-valid-transition [ts]
  (find-first #(= (second %) true)
    (map #(let [{conds :conditions
                 transition :transition
                 on-success :on-success} %]
            [transition (switch-state? conds) on-success]) ts)))

(defn update-state [state]
  (let [transition-list ((meta state) @state)
        [transition _ on-success] (first-valid-transition transition-list)]
    (if-not (nil? transition)
      (do
        (if-not (nil? on-success)
          (on-success))
        (dosync (ref-set state transition))))))

(defmacro until-state [s c & body]
  `(while (not= (deref ~s) ~c)
     ~@body
     (update-state ~s)))