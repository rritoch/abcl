;;; subtypep.lisp
;;;
;;; Copyright (C) 2003 Peter Graves
;;; $Id: subtypep.lisp,v 1.23 2003-11-11 17:37:18 piso Exp $
;;;
;;; This program is free software; you can redistribute it and/or
;;; modify it under the terms of the GNU General Public License
;;; as published by the Free Software Foundation; either version 2
;;; of the License, or (at your option) any later version.
;;;
;;; This program is distributed in the hope that it will be useful,
;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;; GNU General Public License for more details.
;;;
;;; You should have received a copy of the GNU General Public License
;;; along with this program; if not, write to the Free Software
;;; Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

;;; Adapted from GCL.

(in-package "SYSTEM")

(defparameter *known-types* (make-hash-table))

(dolist (i '((ARITHMETIC-ERROR ERROR)
             (ARRAY)
             (BASE-STRING SIMPLE-STRING)
             (BIGNUM INTEGER)
             (BIT INTEGER)
             (BIT-VECTOR VECTOR)
             (BOOLEAN SYMBOL)
             (BUILT-IN-CLASS CLASS)
             (CELL-ERROR ERROR)
             (CHARACTER)
             (CLASS STANDARD-OBJECT)
             (COMPILED-FUNCTION FUNCTION)
             (COMPLEX NUMBER)
             (CONDITION)
             (CONS LIST)
             (CONTROL-ERROR ERROR)
             (DIVISION-BY-ZERO ARITHMETIC-ERROR)
             (END-OF-FILE STREAM-ERROR)
             (ERROR SERIOUS-CONDITION)
             (EXTENDED-CHAR CHARACTER NIL)
             (FILE-ERROR ERROR)
             (FIXNUM INTEGER)
             (FLOAT REAL)
             (FLOATING-POINT-INEXACT ARITHMETIC-ERROR)
             (FLOATING-POINT-INVALID-OPERATION ARITHMETIC-ERROR)
             (FLOATING-POINT-OVERFLOW ARITHMETIC-ERROR)
             (FLOATING-POINT-UNDERFLOW ARITHMETIC-ERROR)
             (FUNCTION)
             (GENERIC-FUNCTION FUNCTION)
             (HASH-TABLE)
             (INTEGER RATIONAL)
             (KEYWORD SYMBOL)
             (LIST SEQUENCE)
             (NULL SYMBOL LIST)
             (NUMBER)
             (PACKAGE)
             (PACKAGE-ERROR ERROR)
             (PARSE-ERROR ERROR)
             (PATHNAME)
             (PRINT-NOT-READABLE ERROR)
             (PROGRAM-ERROR ERROR)
             (RANDOM-STATE)
             (RATIO RATIONAL)
             (RATIONAL REAL)
             (READER-ERROR PARSE-ERROR STREAM-ERROR)
             (READTABLE)
             (REAL NUMBER)
             (RESTART)
             (SERIOUS-CONDITION CONDITION)
             (SIMPLE-ARRAY ARRAY)
             (SIMPLE-BASE-STRING SIMPLE-STRING BASE-STRING)
             (SIMPLE-BIT-VECTOR BIT-VECTOR SIMPLE-ARRAY)
             (SIMPLE-CONDITION CONDITION)
             (SIMPLE-ERROR SIMPLE-CONDITION ERROR)
             (SIMPLE-STRING STRING SIMPLE-ARRAY)
             (SIMPLE-TYPE-ERROR SIMPLE-CONDITION TYPE-ERROR)
             (SIMPLE-VECTOR VECTOR SIMPLE-ARRAY)
             (SIMPLE-WARNING SIMPLE-CONDITION WARNING)
             (STANDARD-CHAR CHARACTER)
             (STANDARD-CLASS CLASS)
             (STANDARD-GENERIC-FUNCTION GENERIC-FUNCTION)
             (STANDARD-OBJECT)
             (STORAGE-CONDITION SERIOUS-CONDITION)
             (STREAM)
             (STREAM-ERROR ERROR)
             (STRING VECTOR)
             (STRUCTURE-CLASS CLASS STANDARD-OBJECT)
             (STYLE-WARNING WARNING)
             (SYMBOL)
             (TWO-WAY-STREAM STREAM)
             (TYPE-ERROR ERROR)
             (UNBOUND-SLOT CELL-ERROR)
             (UNBOUND-VARIABLE CELL-ERROR)
             (UNDEFINED-FUNCTION CELL-ERROR)
             (VECTOR ARRAY SEQUENCE)
             (WARNING CONDITION)
             ))
  (setf (gethash (car i) *known-types*) (cdr i)))

(defun supertypes (type)
  (values (gethash type *known-types*)))

(defun known-type-p (type)
  (multiple-value-bind (value present-p) (gethash type *known-types*)
    present-p))

(defun subtypep-normalize-type (type)
  (when (symbolp type)
    (case type
      (FIXNUM
       (return-from subtypep-normalize-type
                    '(integer #.most-negative-fixnum #.most-positive-fixnum)))
      (BASE-CHAR
       (return-from subtypep-normalize-type 'character))))
  (let (tp i)
    (loop
      (if (consp type)
          (setq tp (car type) i (cdr type))
          (setq tp type i nil))
      (if (and (symbolp tp) (get tp 'deftype-definition))
          (setq type (apply (get tp 'deftype-definition) i))
          (return)))
    (case tp
      ((ARRAY SIMPLE-ARRAY)
       (when (and i (eq (car i) nil)) ; Element type is NIL.
         (if (eq tp 'simple-array)
             (setq tp 'simple-string)
             (setq tp 'string))
         (when (cadr i) ; rank/dimensions
           (cond ((and (consp (cadr i)) (= (length (cadr i)) 1))
                  (if (eq (caadr i) '*)
                      (setq i nil)
                      (setq i (cadr i))))
                 ((eql (cadr i) 1)
                  (setq i nil))
                 (t
                  (error "invalid type specifier ~S" type))))))
      ((SHORT-FLOAT SINGLE-FLOAT DOUBLE-FLOAT LONG-FLOAT)
       (setq tp 'float)))
    (if i (cons tp i) tp)))

(defun sub-interval-p (i1 i2)
  (let (low1 high1 low2 high2)
    (if (null i1)
        (setq low1 '* high1 '*)
        (if (null (cdr i1))
            (setq low1 (car i1) high1 '*)
            (setq low1 (car i1) high1 (cadr i1))))
    (if (null i2)
        (setq low2 '* high2 '*)
        (if (null (cdr i2))
            (setq low2 (car i2) high2 '*)
            (setq low2 (car i2) high2 (cadr i2))))
    (when (and (consp low1) (integerp (car low1)))
      (setq low1 (1+ (car low1))))
    (when (and (consp low2) (integerp (car low2)))
      (setq low2 (1+ (car low2))))
    (when (and (consp high1) (integerp (car high1)))
      (setq high1 (1- (car high1))))
    (when (and (consp high2) (integerp (car high2)))
      (setq high2 (1- (car high2))))
    (cond ((eq low1 '*)
	   (unless (eq low2 '*)
	           (return-from sub-interval-p nil)))
          ((eq low2 '*))
	  ((consp low1)
	   (if (consp low2)
	       (when (< (car low1) (car low2))
		     (return-from sub-interval-p nil))
	       (when (< (car low1) low2)
		     (return-from sub-interval-p nil))))
	  ((if (consp low2)
	       (when (<= low1 (car low2))
		     (return-from sub-interval-p nil))
	       (when (< low1 low2)
		     (return-from sub-interval-p nil)))))
    (cond ((eq high1 '*)
	   (unless (eq high2 '*)
	           (return-from sub-interval-p nil)))
          ((eq high2 '*))
	  ((consp high1)
	   (if (consp high2)
	       (when (> (car high1) (car high2))
		     (return-from sub-interval-p nil))
	       (when (> (car high1) high2)
		     (return-from sub-interval-p nil))))
	  ((if (consp high2)
	       (when (>= high1 (car high2))
		     (return-from sub-interval-p nil))
	       (when (> high1 high2)
		     (return-from sub-interval-p nil)))))
    (return-from sub-interval-p t)))

(defun simple-subtypep (type1 type2)
  (when (and (symbolp type1) (symbolp type2))
    (multiple-value-bind (type1-supertypes type1-known-p)
        (gethash type1 *known-types*)
      (when type1-known-p
        (return-from simple-subtypep (if (memq type2 type1-supertypes)
                                         t
                                         (dolist (supertype type1-supertypes)
                                           (when (simple-subtypep supertype type2)
                                             (return (values t)))))))))
  (let (c1 c2)
    (if (and (setf c1 (if (classp type1) type1 (find-class type1 nil)))
             (setf c2 (if (classp type2) type2 (find-class type2 nil)))
             (memq c2 (class-precedence-list c1)))
        t
        nil)))

(defun subtypep (type1 type2)
  (when (or (eq type1 type2)
            (null type1)
            (eq type2 t))
    (return-from subtypep (values t t)))
  (let* ((c1 (classp type1)) (c2 (classp type2))
         (t1 (if c1
                 type1
                 (and (symbolp type1) (find-class type1 nil))))
         (t2 (if c2
                 type2
                 (and (symbolp type2) (find-class type2 nil)))))
    (when (and t1 t2)
      (return-from subtypep
		   (if (member t2 (class-precedence-list t1))
		       (values t t) (values nil t))))
    (when (or c1 c2)
      (return-from subtypep (values nil t))))
  (setq type1 (subtypep-normalize-type type1)
        type2 (subtypep-normalize-type type2))
  (when (equal type1 type2)
    (return-from subtypep (values t t)))
  (let (t1 t2 i1 i2)
    (if (atom type1)
        (setq t1 type1 i1 nil)
        (setq t1 (car type1) i1 (cdr type1)))
    (if (atom type2)
        (setq t2 type2 i2 nil)
        (setq t2 (car type2) i2 (cdr type2)))
    (when (eq t2 'atom)
      (return-from subtypep (cond ((memq t1 '(cons list)) (values nil t))
                                  ((known-type-p t1) (values t t))
                                  (t (values nil nil)))))
    (cond ((eq t1 'member)
           (dolist (e i1)
             (unless (typep e type2) (return-from subtypep (values nil t))))
           (return-from subtypep (values t t)))
          ((eq t1 'or)
           (dolist (tt i1)
             (multiple-value-bind (tv flag) (subtypep tt type2)
               (unless tv (return-from subtypep (values tv flag)))))
           (return-from subtypep (values t t)))
          ((eq t1 'and)
           (dolist (tt i1)
             (let ((tv (subtypep tt type2)))
               (when tv (return-from subtypep (values t t)))))
           (return-from subtypep (values nil nil)))
          ((eq t2 'or)
           (dolist (tt i2)
             (let ((tv (subtypep type1 tt)))
               (when tv (return-from subtypep (values t t)))))
           (return-from subtypep (values nil nil)))
          ((eq t2 'and)
           (dolist (tt i2)
             (multiple-value-bind (tv flag) (subtypep type1 tt)
               (unless tv (return-from subtypep (values tv flag)))))
           (return-from subtypep (values t t)))
          ((null (or i1 i2))
           (return-from subtypep (values (simple-subtypep t1 t2) t)))
          ((eq t2 'sequence)
           (cond ((memq t1 '(null cons list))
                  (values t t))
                 ((memq t1 '(array simple-array))
                  (if (and (cdr i1) (consp (cadr i1)) (null (cdadr i1)))
                      (values t t)
                      (values nil t)))
                 (t (values nil (known-type-p t1)))))
          ((eq t2 'vector)
           (if (eq t1 'base-string)
               (if (eq (car i2) 'base-char)
                   (values t t)
                   (values nil t))
               (values nil (known-type-p t2))))
          ((eq t2 'simple-string)
           (if (memq t1 '(simple-string simple-base-string))
               (if (or (null i2) (eq (car i2) '*))
                   (values t t)
                   (values nil t))
               (values nil (known-type-p t2))))
          ((eq t2 'base-string)
           (if (eq t1 'vector)
               (if (eq (car i1) 'base-char)
                   (values t t)
                   (values nil t))
               (values nil (known-type-p t2))))
          ((eq t2 'string)
           (if (eq t1 'vector)
               (if (eq (car i1) 'character)
                   (values t t)
                   (values nil t))
               (values nil (known-type-p t2))))
          (t
           (cond ((eq t1 'float)
                  (if (memq t2 '(float real number))
                      (values (sub-interval-p i1 i2) t)
                      (values nil (known-type-p t2))))
                 ((eq t1 'integer)
                  (if (memq t2 '(integer rational real number))
                      (values (sub-interval-p i1 i2) t)
                      (values nil (known-type-p t2))))
                 ((eq t1 'rational)
                  (if (memq t2 '(rational real number))
                      (values (sub-interval-p i1 i2) t)
                      (values nil (known-type-p t2))))
                 ((eq t1 'real)
                  (if (memq t2 '(real number))
                      (values (sub-interval-p i1 i2) t)
                      (values nil (known-type-p t2))))
                 ((memq t1 '(string simple-string base-string
                             simple-base-string))
                  (cond ((eq t2 'string)
                         (if (or (null i2) (eq (car i2) '*))
                             (values t t)
                             (values nil t)))
                        (t
                         (values nil (known-type-p t2)))))
                 (t
                  (values nil nil)))))))

(when (fboundp 'jvm::jvm-compile)
  (mapcar #'jvm::jvm-compile '(subtypep-normalize-type
                               sub-interval-p
                               simple-subtypep
                               subtypep)))
