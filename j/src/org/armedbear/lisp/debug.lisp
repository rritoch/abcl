;;; debug.lisp
;;;
;;; Copyright (C) 2003 Peter Graves
;;; $Id: debug.lisp,v 1.4 2003-10-01 13:59:23 piso Exp $
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

(in-package "SYSTEM")

(defun debug-prompt (stream)
  (format stream "~%> "))

(defun invoke-debugger (condition)
  (when *debugger-hook*
    (let ((hook-function *debugger-hook*)
          (*debugger-hook* nil))
      (funcall hook-function condition hook-function)))
  (catch 'continue
    (debugger-loop)))

(defun debugger-loop ()
  (fresh-line *debug-io*)
  (format *debug-io* "Type :continue to continue...")
  (loop
    (catch 'debug-loop-catcher
      (handler-bind ((error (lambda (condition)
                              (format *debug-io* "~%Error: ~S.~%" condition)
                              (throw 'debug-loop-catcher nil))))
        (debug-prompt *debug-io*)
        (finish-output *debug-io*)
        (let* ((exp (read *debug-io*)))
          (when (memq exp '(:co :continue))
            (return-from debugger-loop))
          (let* ((values (multiple-value-list (eval exp)))
                 (*standard-output* *debug-io*))
            (fresh-line)
            (if values (prin1 (car values)))
            (dolist (x (cdr values))
              (fresh-line)
              (prin1 x))))))))

(defun break (&optional format-control &rest format-arguments)
  (fresh-line *debug-io*)
  (format *debug-io* "break called~%")
  (invoke-debugger nil))
